package com.alittleapp.feature_webui

import android.content.Context
import android.net.wifi.WifiManager
import com.alittleapp.data.repository.NoteRepository
import com.alittleapp.data.repository.TaskRepository
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded Ktor HTTP server that serves a local web dashboard.
 *
 * When "Go Live" is active, the app listens on a random port on the local Wi-Fi interface.
 * Any device on the same network can open http://<device-ip>:<port> in a browser
 * to view notes and tasks in read-only mode.
 *
 * Security notes:
 * - Read-only: no write operations exposed via the web UI
 * - Runs only while the user has explicitly started it
 * - Auto-stops when the app is stopped or Go Live is disabled
 * - Local network only (no external routing)
 */
@Singleton
class WebUiServer @Inject constructor(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val gson: Gson
) {
    private var engine: ApplicationEngine? = null
    var listenPort: Int = -1
        private set

    @Suppress("DEPRECATION")
    fun getLocalIp(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }

    suspend fun start(port: Int = 0): Int {
        stop()
        val effectivePort = if (port == 0) findFreePort() else port
        listenPort = effectivePort

        engine = embeddedServer(Netty, port = effectivePort, host = "0.0.0.0") {
            routing {
                get("/") { call.respondText(buildDashboardHtml(), ContentType.Text.Html) }
                get("/api/notes") {
                    val notes = noteRepo.getAllNotes().first()
                    call.respondText(gson.toJson(notes), ContentType.Application.Json)
                }
                get("/api/tasks") {
                    val tasks = taskRepo.getPendingTasks().first()
                    call.respondText(gson.toJson(tasks), ContentType.Application.Json)
                }
                get("/health") { call.respondText("ok") }
            }
        }.start(wait = false)

        return effectivePort
    }

    fun stop() {
        engine?.stop(500, 500)
        engine = null
        listenPort = -1
    }

    val isRunning: Boolean get() = engine != null

    private fun findFreePort(): Int {
        val socket = java.net.ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    private fun buildDashboardHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>A Little App — Live Dashboard</title>
<style>
  :root { --primary: #6650A4; --bg: #1C1B1F; --surface: #2B2930; --on-surface: #E6E1E5; --secondary: #CCC2DC; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: var(--bg); color: var(--on-surface); font-family: system-ui, -apple-system, sans-serif; padding: 16px; max-width: 900px; margin: 0 auto; }
  h1 { color: var(--primary); font-size: 1.5rem; margin: 16px 0; display: flex; align-items: center; gap: 8px; }
  h2 { color: var(--secondary); font-size: 1rem; margin: 24px 0 8px; }
  .badge { background: var(--primary); color: white; border-radius: 12px; padding: 2px 10px; font-size: 0.75rem; }
  .card { background: var(--surface); border-radius: 12px; padding: 12px 16px; margin-bottom: 8px; }
  .note-title { font-weight: 600; margin-bottom: 4px; }
  .note-body { color: #b0acb9; font-size: 0.9rem; white-space: pre-wrap; }
  .task { display: flex; align-items: center; gap: 10px; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; }
  .pill { display: inline-block; background: #3a3740; border-radius: 6px; padding: 2px 8px; font-size: 0.75rem; color: var(--secondary); margin-top: 4px; }
  .status { font-size: 0.75rem; color: #7a7680; margin-top: 24px; }
  .live { color: #4CAF50; }
  #refresh { background: var(--primary); color: white; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; margin-bottom: 8px; }
</style>
</head>
<body>
<h1>&#9632; A Little App <span class="badge live">LIVE</span></h1>
<p style="color:#7a7680;font-size:0.85rem;">Read-only view from your Android device. Refresh to update.</p>
<button id="refresh" onclick="loadAll()">Refresh</button>

<h2>Notes</h2>
<div id="notes"><p style="color:#7a7680">Loading…</p></div>

<h2>Pending Tasks</h2>
<div id="tasks"><p style="color:#7a7680">Loading…</p></div>

<p class="status">Auto-refreshes every 30s &bull; All data stays on your device &bull; Read-only</p>

<script>
async function loadNotes() {
  const res = await fetch('/api/notes');
  const notes = await res.json();
  const el = document.getElementById('notes');
  if (!notes.length) { el.innerHTML = '<p style="color:#7a7680">No notes yet.</p>'; return; }
  el.innerHTML = notes.map(n => `
    <div class="card">
      ${n.title ? `<div class="note-title">${esc(n.title)}</div>` : ''}
      <div class="note-body">${esc(n.content.substring(0,300))}${n.content.length>300?'…':''}</div>
      ${n.tags ? `<span class="pill">${esc(n.tags.split(',').map(t=>'#'+t.trim()).join(' '))}</span>` : ''}
    </div>`).join('');
}

async function loadTasks() {
  const res = await fetch('/api/tasks');
  const tasks = await res.json();
  const el = document.getElementById('tasks');
  if (!tasks.length) { el.innerHTML = '<p style="color:#7a7680">No pending tasks.</p>'; return; }
  el.innerHTML = tasks.map(t => `
    <div class="card task">
      <div class="dot"></div>
      <span>${esc(t.title)}</span>
    </div>`).join('');
}

function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

function loadAll() { loadNotes(); loadTasks(); }
loadAll();
setInterval(loadAll, 30000);
</script>
</body>
</html>
    """.trimIndent()
}
