use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// Phone → Cloud: an event from the user's device.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceEvent {
    #[serde(rename = "type")]
    pub event_type: EventType,
    pub timestamp: DateTime<Utc>,
    pub source: EventSource,
    pub device: DeviceContext,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum EventType {
    Notification,
    UserCommand,
    Schedule,
    Location,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EventSource {
    /// Android package name, e.g. "com.tencent.mm"
    pub app: String,
    pub title: Option<String>,
    pub text: String,
    /// Who sent it (contact name or group member), if known
    pub sender: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceContext {
    pub battery: u8,
    pub network: NetworkType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum NetworkType {
    Wifi,
    Cellular,
    Offline,
}
