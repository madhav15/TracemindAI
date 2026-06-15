# MCP Server

Spring AI MCP Server for Claude Desktop integration — exposes tools that query Splunk for TraceMind document processing timelines.

## Prerequisites

- Java 21
- Splunk instance accessible from the server host

## Configuration

All configuration is externalized. Set the following environment variables before launching:

| Variable           | Required | Default | Description                |
|--------------------|----------|---------|----------------------------|
| `SPLUNK_BASE_URL`  | Yes      | —       | Splunk API base URL (e.g. `https://splunk.example.com:8089`) |
| `SPLUNK_USERNAME`  | Yes      | —       | Splunk username            |
| `SPLUNK_PASSWORD`  | Yes      | —       | Splunk password            |
| `SPLUNK_INDEX`     | No       | `tracemind` | Splunk index name      |
| `SPLUNK_VERIFY_SSL` | No      | `false`  | Whether to verify SSL certificates |

## Build

```bash
cd mcp-server
mvn clean package -DskipTests
```

The executable JAR is produced at `target/mcp-server-1.0.0-SNAPSHOT.jar`.

---

## Local Claude Desktop MCP Setup

### 1. Start the MCP server

The JAR uses **stdio transport** (no port, no HTTP server). Claude Desktop launches it directly as a subprocess.

The exact command to start the JAR from a terminal:

```bash
java -jar mcp-server/target/mcp-server-1.0.0-SNAPSHOT.jar
```

**Working directory:** `/Users/madhav/Projects/java/TracemindAI` (project root)

**Required environment variables:**

```bash
export SPLUNK_BASE_URL=https://<your-splunk-host>:8089
export SPLUNK_USERNAME=<your-splunk-username>
export SPLUNK_PASSWORD=<your-splunk-password>
export SPLUNK_INDEX=tracemind
```

**Expected startup message (on stderr):**

```
=== TraceMind MCP Server ===
Server: tracemind-mcp-server v1.0.0
Transport: stdio
Splunk target: https://<your-splunk-host>:8089
Splunk index: tracemind
MCP server ready — waiting for Claude Desktop connection
```

### 2. Claude Desktop configuration

Add the following to your Claude Desktop `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "tracemind": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/madhav/Projects/java/TracemindAI/mcp-server/target/mcp-server-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "SPLUNK_BASE_URL": "https://<your-splunk-host>:8089",
        "SPLUNK_USERNAME": "<your-splunk-username>",
        "SPLUNK_PASSWORD": "<your-splunk-password>",
        "SPLUNK_INDEX": "tracemind"
      }
    }
  }
}
```

**Configuration file location:**
- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux:** `~/.config/Claude/claude_desktop_config.json`

### 3. Verify the tool is visible

After restarting Claude Desktop, click the **"tools"** (hammer) icon in the input area. You should see one tool listed:

```
get_job_timeline  —  tracemind (local)
```

If you don't see it:
- Check Claude Desktop logs: **Help → Open MCP Logs**
- Verify the JAR path and JSON syntax in `claude_desktop_config.json`
- Confirm the environment variables are correct (Splunk credentials)
- Test the JAR directly from a terminal (see step 1 above)

---

## Available Tools

### `get_job_timeline`

Returns the complete processing timeline for a given job ID by querying Splunk.

| Parameter | Type   | Required | Description                         |
|-----------|--------|----------|-------------------------------------|
| `jobId`   | String | Yes      | The job ID to retrieve the timeline for |

---

## Transport Details

- **Protocol:** MCP over stdio
- **Framework:** Spring AI MCP Server (`spring-ai-starter-mcp-server`)
- **Server type:** SYNC
- **MCP version:** 1.0.0 (Spring AI M7)
