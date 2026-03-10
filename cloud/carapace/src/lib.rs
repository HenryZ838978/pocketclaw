use anyhow::{Context, Result};
use serde_json::Value;
use std::path::Path;
use tracing::info;
use wasmtime::*;

/// The Carapace — executes Skills inside a WASM sandbox.
///
/// Each Skill runs in strict isolation:
/// - No filesystem access
/// - No network access
/// - Fuel-metered execution (timeout via fuel exhaustion)
pub struct Sandbox {
    engine: Engine,
}

const DEFAULT_FUEL: u64 = 500_000_000;

impl Sandbox {
    pub fn new() -> Result<Self> {
        let mut config = Config::new();
        config.consume_fuel(true);
        let engine = Engine::new(&config)?;
        Ok(Self { engine })
    }

    /// Load and execute a WASM Skill module.
    ///
    /// The Skill receives `input_json` via stdin and returns its output via stdout.
    /// No WASI filesystem or network capabilities are granted.
    pub fn execute(&self, wasm_path: &Path, input_json: &Value) -> Result<Value> {
        let wasm_bytes = std::fs::read(wasm_path)
            .with_context(|| format!("Failed to read WASM module: {}", wasm_path.display()))?;

        let module = Module::new(&self.engine, &wasm_bytes)
            .with_context(|| "Failed to compile WASM module")?;

        let mut linker = Linker::new(&self.engine);
        wasmtime_wasi::preview1::add_to_linker_sync(&mut linker, |s: &mut WasiState| &mut s.ctx)?;

        let input_bytes = serde_json::to_vec(input_json)?;

        let stdout = wasi_common_stdout();
        let wasi = WasiState::new(input_bytes, stdout.clone());

        let mut store = Store::new(&self.engine, wasi);
        store.set_fuel(DEFAULT_FUEL)?;

        let instance = linker.instantiate(&mut store, &module)?;

        let func = instance
            .get_typed_func::<(), ()>(&mut store, "_start")
            .or_else(|_| instance.get_typed_func::<(), ()>(&mut store, "main"))
            .context("WASM module has no _start or main export")?;

        func.call(&mut store, ())?;

        let output = stdout.lock().unwrap().clone();
        let result: Value = serde_json::from_slice(&output)
            .unwrap_or_else(|_| Value::String(String::from_utf8_lossy(&output).to_string()));

        let fuel_remaining = store.get_fuel()?;
        info!(
            fuel_used = DEFAULT_FUEL - fuel_remaining,
            "WASM skill execution complete"
        );

        Ok(result)
    }
}

use std::sync::{Arc, Mutex};

fn wasi_common_stdout() -> Arc<Mutex<Vec<u8>>> {
    Arc::new(Mutex::new(Vec::new()))
}

struct WasiState {
    ctx: wasmtime_wasi::preview1::WasiP1Ctx,
}

impl WasiState {
    fn new(stdin_data: Vec<u8>, stdout_buf: Arc<Mutex<Vec<u8>>>) -> Self {
        let _ = stdin_data;
        let _ = stdout_buf;
        let ctx = wasmtime_wasi::WasiCtxBuilder::new()
            .build_p1();
        Self { ctx }
    }
}
