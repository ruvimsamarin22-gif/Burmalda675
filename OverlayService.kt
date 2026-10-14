package com.example.burmalda

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

// ---------- модель меню (всё визуальное, ничего не делает) ----------
private sealed class It
private class Hd(val t: String) : It()
private class Tg(val n: String, var on: Boolean = false, val fx: ((Boolean) -> Unit)? = null) : It()
private class Sl(val n: String, val min: Int, val max: Int, var v: Int, val u: String = "",
                 val fx: ((Int) -> Unit)? = null) : It()
private class Ch(val n: String, val o: List<String>, var i: Int = 0, val fx: ((Int) -> Unit)? = null) : It()
private class Cp(val n: String, var v: Int, val fx: (Int) -> Unit) : It()
private class Bt(val n: String, val fx: () -> Unit) : It()
private object Con : It()
private class Sk(val w: Map<String, List<Pair<String, String>>>) : It() {
    val sel = HashMap<String, String>()
    var cur: String = w.keys.first()
}

private fun sk(vararg a: String) = a.map { val p = it.split(":"); p[0] to p[1] }
private val PALETTE = listOf("#ff4d4d", "#4ade80", "#22d3ee", "#a78bfa", "#fde047", "#f97316", "#ffffff")
    .map { Color.parseColor(it) }

// ---------- HUD поверх игры: FOV-круг, ESP, радар (всё нарисовано, ничего не читает из игры) ----------
private class Hud(ctx: Context) : View(ctx) {
    var fov = true; var fovV = 60; var fovFill = false; var fovCol = Color.parseColor("#ff4d4d")
    var esp = true; var corner = false; var names = true; var hp = true; var dist = true
    var lines = false; var skel = false; var cross = true; var wm = true; var radar = true; var count = true
    var espCol = Color.parseColor("#ff4d4d"); var accent = Color.parseColor("#4ade80")

    private class En(var x: Float, var y: Float, var vx: Float, val n: String, val hp: Float, val s: Float)
    private val ens = ArrayList<En>()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = java.util.Random()
    private val nm = listOf("Wolf", "Ghost", "Kiro", "Zed", "Nova", "Rex")
    private var fps = 60; private var ping = 23; private var lastT = 0L; private var sweep = 0f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        ens.clear()
        val d = resources.displayMetrics.density
        for (i in 0 until 5) ens.add(En(
            w * (0.15f + 0.7f * rnd.nextFloat()), h * (0.50f + 0.12f * rnd.nextFloat()),
            (if (rnd.nextBoolean()) 1 else -1) * (0.5f + rnd.nextFloat()) * d,
            nm[i], 0.35f + 0.65f * rnd.nextFloat(), 0.75f + 0.5f * rnd.nextFloat()))
    }

    private fun line(c: Canvas, a: Float, b: Float, x: Float, y: Float) = c.drawLine(a, b, x, y, p)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        val now = System.currentTimeMillis()
        if (now - lastT > 1000) { fps = 57 + rnd.nextInt(4); ping = 18 + rnd.nextInt(20); lastT = now }
        sweep += 0.06f

        if (fov) {
            val r = min(fovV / 120f * 0.5f * min(w, h), 0.5f * min(w, h))
            if (fovFill) { p.style = Paint.Style.FILL; p.color = (fovCol and 0xFFFFFF) or 0x22000000; c.drawCircle(w / 2, h / 2, r, p) }
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f * d; p.color = fovCol
            c.drawCircle(w / 2, h / 2, r, p)
        }
        if (cross) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f * d; p.color = Color.WHITE
            val g = 3 * d; val l = 8 * d
            line(c, w / 2 - g - l, h / 2, w / 2 - g, h / 2); line(c, w / 2 + g, h / 2, w / 2 + g + l, h / 2)
            line(c, w / 2, h / 2 - g - l, w / 2, h / 2 - g); line(c, w / 2, h / 2 + g, w / 2, h / 2 + g + l)
        }
        for (e in ens) {
            e.x += e.vx
            if (e.x < w * 0.1f || e.x > w * 0.9f) e.vx = -e.vx
        }
        if (esp) for (e in ens) {
            val bh = 88f * d * e.s; val bw = bh * 0.42f
            val l = e.x - bw / 2; val t = e.y - bh / 2; val r = l + bw; val b = t + bh
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f * d; p.color = espCol
            if (corner) {
                val k = bw * 0.28f; val kv = bh * 0.2f
                line(c, l, t, l + k, t); line(c, l, t, l, t + kv); line(c, r, t, r - k, t); line(c, r, t, r, t + kv)
                line(c, l, b, l + k, b); line(c, l, b, l, b - kv); line(c, r, b, r - k, b); line(c, r, b, r, b - kv)
            } else c.drawRect(l, t, r, b, p)
            if (lines) line(c, w / 2, h, e.x, b)
            if (skel) {
                c.drawCircle(e.x, t + bh * 0.1f, bw * 0.14f, p)
                line(c, e.x, t + bh * 0.2f, e.x, t + bh * 0.55f)
                line(c, e.x - bw * 0.4f, t + bh * 0.3f, e.x + bw * 0.4f, t + bh * 0.3f)
                line(c, e.x, t + bh * 0.55f, e.x - bw * 0.25f, b); line(c, e.x, t + bh * 0.55f, e.x + bw * 0.25f, b)
            }
            p.style = Paint.Style.FILL
            if (hp) {
                p.color = Color.parseColor("#88000000"); c.drawRect(l - 6 * d, t, l - 3 * d, b, p)
                p.color = if (e.hp > .66f) Color.parseColor("#4ade80") else if (e.hp > .33f) Color.parseColor("#fde047") else Color.parseColor("#f87171")
                c.drawRect(l - 6 * d, b - bh * e.hp, l - 3 * d, b, p)
            }
            p.textSize = 11 * d; p.textAlign = Paint.Align.CENTER; p.color = Color.WHITE
            p.setShadowLayer(3f, 0f, 0f, Color.BLACK)
            if (names) c.drawText(e.n, e.x, t - 5 * d, p)
            if (dist) c.drawText("${(30 + e.x / w * 120).toInt()} m", e.x, b + 13 * d, p)
            p.clearShadowLayer()
        }
        p.style = Paint.Style.FILL; p.typeface = Typeface.MONOSPACE; p.textSize = 12 * d
        p.setShadowLayer(3f, 0f, 0f, Color.BLACK)
        if (wm) { p.textAlign = Paint.Align.LEFT; p.color = accent; c.drawText("BURMALDA · $fps FPS · $ping ms", 48 * d, 24 * d, p) }
        if (count) { p.textAlign = Paint.Align.CENTER; p.color = Color.WHITE; c.drawText("Врагов рядом: ${ens.size}", w / 2, 24 * d, p) }
        p.clearShadowLayer(); p.typeface = Typeface.DEFAULT
        if (radar) {
            val rr = 52 * d; val cx = w - rr - 48 * d; val cy = rr + 14 * d
            p.style = Paint.Style.FILL; p.color = Color.parseColor("#66000000"); c.drawCircle(cx, cy, rr, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1f * d; p.color = Color.parseColor("#33ffffff")
            c.drawCircle(cx, cy, rr, p); c.drawCircle(cx, cy, rr / 2, p)
            line(c, cx - rr, cy, cx + rr, cy); line(c, cx, cy - rr, cx, cy + rr)
            p.color = accent; line(c, cx, cy, cx + rr * cos(sweep), cy + rr * sin(sweep))
            p.style = Paint.Style.FILL
            for (e in ens) {
                p.color = Color.parseColor("#f87171")
                c.drawCircle(cx + (e.x - w / 2) / w * rr * 1.8f, cy + (e.y - h / 2) / h * rr * 3f, 3 * d, p)
            }
            p.color = accent; c.drawCircle(cx, cy, 3.5f * d, p)
        }
        postInvalidateDelayed(33)
    }
}

class OverlayService : Service() {
    companion object {
        const val EXTRA_PKG = "pkg"
        const val ACTION_STOP = "stop"
        const val SCARE_DELAY_MS = 120_000L   // 2 минуты после запуска
        const val SCARE_MS = 6_000L           // длительность пугалки
        private const val CH = "burmalda"
    }

    private lateinit var wm: WindowManager
    private val h = Handler(Looper.getMainLooper())
    private var accent = Color.parseColor("#4ade80")
    private var gameLabel = ""
    private var hud: Hud? = null
    private var bubble: View? = null
    private var accentLine: View? = null
    private var menuView: View? = null
    private var menuShown = false
    private var scareView: View? = null
    private var revealView: View? = null
    private var track: AudioTrack? = null
    private var flashRun: Runnable? = null
    private var conRun: Runnable? = null
    private lateinit var data: LinkedHashMap<String, List<It>>
    private lateinit var menuLp: WindowManager.LayoutParams
    private val scareRun = Runnable { showScare() }
    private val endScareRun = Runnable { endScare() }

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startFg()
        if (bubble != null) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val pkg = intent?.getStringExtra(EXTRA_PKG) ?: ""
        val sf = pkg.contains("standoff")
        accent = Color.parseColor(if (sf) "#f97316" else "#4ade80")
        gameLabel = if (sf) "Standoff 2" else "PUBG Mobile"
        val hv = Hud(this).also { it.accent = accent }
        hud = hv
        wm.addView(hv, lp(-1, -1, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
        data = menuFor(sf)
        for (items in data.values) for (item in items) when (item) {
            is Tg -> item.fx?.invoke(item.on)
            is Sl -> item.fx?.invoke(item.v)
            is Ch -> item.fx?.invoke(item.i)
            is Cp -> item.fx(item.v)
            else -> {}
        }
        buildBubble(); buildMenu()
        h.postDelayed(scareRun, SCARE_DELAY_MS)
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "BURMALDA", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("BURMALDA работает")
            .setContentText("Нажми «Остановить», чтобы закрыть оверлей")
            .addAction(Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_delete), "Остановить", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    // ---------- окна ----------
    private fun lp(w: Int, hh: Int, flags: Int) = WindowManager.LayoutParams(
        w, hh, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT)

    private fun tv(t: String, size: Float = 14f, color: Int = Color.WHITE) =
        TextView(this).apply { text = t; textSize = size; setTextColor(color) }

    private fun pill(color: Int, r: Int = 12) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat()
    }

    private fun draggable(handle: View, target: View, p: WindowManager.LayoutParams, onClick: (() -> Unit)? = null) {
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    p.x = ox + dx.toInt(); p.y = oy + dy.toInt()
                    wm.updateViewLayout(target, p); true
                }
                MotionEvent.ACTION_UP -> { if (!moved) onClick?.invoke(); true }
                else -> false
            }
        }
    }

    private fun ovalBg(col: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col) }

    private fun buildBubble() {
        val v = TextView(this).apply {
            text = "BM"; gravity = Gravity.CENTER; setTextColor(Color.BLACK); textSize = 14f
            typeface = Typeface.DEFAULT_BOLD; background = ovalBg(accent)
        }
        val p = lp(dp(48), dp(48), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(80)
        }
        draggable(v, v, p) { toggleMenu() }
        wm.addView(v, p); bubble = v
    }

    private fun buildMenu() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = pill(Color.parseColor("#E00e0f1e"), 14).apply { setStroke(dp(1), Color.parseColor("#1FFFFFFF")) }
        }
        val line = View(this).apply { setBackgroundColor(accent) }
        accentLine = line
        root.addView(line, LinearLayout.LayoutParams(-1, dp(2)))
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(8), dp(6))
        }
        head.addView(tv("BURMALDA", 16f).apply { typeface = Typeface.DEFAULT_BOLD })
        head.addView(tv("v3.2 · $gameLabel", 11f, Color.parseColor("#99ffffff")).apply { setPadding(dp(8), dp(3), 0, 0) },
            LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(Button(this).apply { text = "–"; setOnClickListener { toggleMenu() } },
            LinearLayout.LayoutParams(dp(44), dp(40)))
        root.addView(head)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(12), dp(12))
        }
        val tabBtns = LinkedHashMap<String, TextView>()
        fun open(name: String) {
            tabBtns.forEach { (n, b) ->
                b.setTextColor(if (n == name) accent else Color.parseColor("#99ffffff"))
                b.typeface = if (n == name) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            render(data[name]!!, content)
        }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), 0, dp(6), 0) }
        data.keys.forEach { n ->
            val b = tv(n, 13f).apply { setPadding(dp(10), dp(8), dp(10), dp(8)); setOnClickListener { open(n) } }
            tabBtns[n] = b; bar.addView(b)
        }
        root.addView(HorizontalScrollView(this).apply { addView(bar); isHorizontalScrollBarEnabled = false })
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        open(data.keys.first())

        val maxH = (resources.displayMetrics.heightPixels * 0.85).toInt()
        menuLp = lp(dp(330), min(dp(460), maxH),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(80); y = dp(24)
        }
        draggable(head, root, menuLp)
        menuView = root
    }

    private fun toggleMenu() {
        val v = menuView ?: return
        if (menuShown) wm.removeView(v) else wm.addView(v, menuLp)
        menuShown = !menuShown
    }

    private fun render(items: List<It>, box: LinearLayout) {
        box.removeAllViews()
        conRun?.let { h.removeCallbacks(it) }; conRun = null
        for (item in items) when (item) {
            is Hd -> box.addView(tv(item.t.uppercase(), 11f, accent).apply {
                typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12); bottomMargin = dp(2) })
            is Tg -> {
                val r = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(3), 0, dp(3))
                }
                r.addView(tv(item.n), LinearLayout.LayoutParams(0, -2, 1f))
                r.addView(Switch(this).apply {
                    isChecked = item.on
                    setOnCheckedChangeListener { _, c -> item.on = c; item.fx?.invoke(c) }
                    val st = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                    thumbTintList = ColorStateList(st, intArrayOf(accent, Color.LTGRAY))
                    trackTintList = ColorStateList(st, intArrayOf(Color.parseColor("#664ade80"), Color.parseColor("#33ffffff")))
                })
                box.addView(r)
            }
            is Sl -> {
                val lab = tv("", 13f)
                fun upd() { lab.text = "${item.n}: ${item.v}${item.u}" }
                upd()
                box.addView(lab, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
                box.addView(SeekBar(this).apply {
                    max = item.max - item.min; progress = item.v - item.min
                    progressTintList = ColorStateList.valueOf(accent); thumbTintList = ColorStateList.valueOf(accent)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                            item.v = p + item.min; upd(); item.fx?.invoke(item.v)
                        }
                        override fun onStartTrackingTouch(s: SeekBar?) {}
                        override fun onStopTrackingTouch(s: SeekBar?) {}
                    })
                })
            }
            is Ch -> {
                box.addView(tv(item.n, 13f), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
                val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                fun draw() {
                    chips.removeAllViews()
                    item.o.forEachIndexed { idx, name ->
                        val on = idx == item.i
                        chips.addView(tv(name, 12f, if (on) Color.BLACK else Color.WHITE).apply {
                            setPadding(dp(11), dp(6), dp(11), dp(6))
                            background = pill(if (on) accent else Color.parseColor("#22ffffff"), 16)
                            setOnClickListener { item.i = idx; item.fx?.invoke(idx); draw() }
                        }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6); topMargin = dp(4) })
                    }
                }
                draw()
                box.addView(HorizontalScrollView(this).apply { addView(chips); isHorizontalScrollBarEnabled = false })
            }
            is Cp -> {
                box.addView(tv(item.n, 13f), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                fun draw() {
                    row.removeAllViews()
                    PALETTE.forEach { col ->
                        row.addView(View(this).apply {
                            background = ovalBg(col).apply { if (col == item.v) setStroke(dp(2), Color.WHITE) }
                            setOnClickListener { item.v = col; item.fx(col); draw() }
                        }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(8); topMargin = dp(4) })
                    }
                }
                draw(); box.addView(row)
            }
            is Bt -> box.addView(Button(this).apply { text = item.n; setOnClickListener { item.fx() } })
            is Sk -> {
                val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                box.addView(c); renderSkins(item, c)
            }
            is Con -> {
                val pool = listOf("[ESP] entities: 5", "[AIM] target locked", "[AIM] target lost", "[NET] ping 31 ms",
                    "[BM] anti-ban: ok", "[BM] hwid spoof: ok", "[MEM] scan complete", "[ESP] loot nearby: 3",
                    "[BM] heartbeat ok", "[NET] packet filter: on", "[AIM] fov check: ok", "[BM] overlay attached")
                val t = TextView(this).apply {
                    textSize = 11f; typeface = Typeface.MONOSPACE; setTextColor(Color.parseColor("#bbffffff"))
                    background = pill(Color.parseColor("#59000000"), 10); setPadding(dp(8), dp(8), dp(8), dp(8))
                    minHeight = dp(150)
                }
                val lines = ArrayDeque<String>()
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                fun add() {
                    lines.addLast(fmt.format(Date()) + " " + pool[(Math.random() * pool.size).toInt()])
                    while (lines.size > 9) lines.removeFirst()
                    t.text = lines.joinToString("\n")
                }
                repeat(5) { add() }
                val r = object : Runnable { override fun run() { add(); h.postDelayed(this, 700) } }
                conRun = r; h.postDelayed(r, 700)
                box.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            }
        }
    }

    private fun renderSkins(s: Sk, parent: LinearLayout) {
        parent.removeAllViews()
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        s.w.keys.forEach { n ->
            chips.addView(Button(this).apply {
                text = n; isAllCaps = false; textSize = 12f
                setTextColor(if (n == s.cur) accent else Color.WHITE)
                setOnClickListener { s.cur = n; renderSkins(s, parent) }
            })
        }
        parent.addView(HorizontalScrollView(this).apply { addView(chips); isHorizontalScrollBarEnabled = false })
        parent.addView(tv("Текущий: ${s.sel[s.cur] ?: "стандартный"}", 12f, Color.parseColor("#aaffffff")))
        s.w[s.cur]!!.forEach { (name, col) ->
            val chosen = s.sel[s.cur] == name
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = pill(if (chosen) Color.parseColor("#334ade80") else Color.parseColor("#22ffffff"), 10)
                setOnClickListener {
                    s.sel[s.cur] = name
                    Toast.makeText(this@OverlayService, "${s.cur}: $name применён", Toast.LENGTH_SHORT).show()
                    renderSkins(s, parent)
                }
            }
            r.addView(View(this).apply { background = pill(Color.parseColor(col), 6) }, LinearLayout.LayoutParams(dp(36), dp(24)))
            r.addView(tv(name, 14f).apply { setPadding(dp(10), 0, 0, 0) })
            parent.addView(r, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
        parent.addView(Button(this).apply {
            text = "Сбросить скин"; setOnClickListener { s.sel.remove(s.cur); renderSkins(s, parent) }
        })
    }

    // ---------- пугалка ----------
    private fun showScare() {
        if (menuShown) toggleMenu()
        val reduce = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        val red = Color.parseColor("#e00000"); val blue = Color.parseColor("#0030ff")
        val v = FrameLayout(this).apply {
            setBackgroundColor(red)
            addView(TextView(this@OverlayService).apply {
                text = "⚠ ЧИТЕР ОБНАРУЖЕН ⚠\n\nнажми, чтобы закрыть"
                gravity = Gravity.CENTER; textSize = 30f; setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }, FrameLayout.LayoutParams(-1, -1))
            setOnClickListener { endScare() }
        }
        wm.addView(v, lp(-1, -1, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON))
        scareView = v
        if (!reduce) { // ~1.7 вспышки в секунду, безопасно для глаз
            var isRed = true
            flashRun = object : Runnable {
                override fun run() { isRed = !isRed; v.setBackgroundColor(if (isRed) red else blue); h.postDelayed(this, 300) }
            }.also { h.postDelayed(it, 300) }
        }
        playScream()
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 150, 500, 150, 500, 150, 1000), -1))
        h.postDelayed(endScareRun, SCARE_MS)
    }

    private fun endScare() {
        h.removeCallbacks(endScareRun)
        flashRun?.let { h.removeCallbacks(it) }; flashRun = null
        stopScream()
        @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        scareView?.let { try { wm.removeView(it) } catch (_: Exception) {} }; scareView = null
        if (revealView != null) return
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F004030e")); setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        v.addView(tv("Это был розыгрыш", 26f).apply { typeface = Typeface.DEFAULT_BOLD })
        v.addView(tv("Никаких читов здесь нет: переключатели, FOV, ESP и скины ничего не делают. " +
            "Читы приводят к бану аккаунта, а скачанные «клиенты» часто воруют данные. Играй честно.",
            15f, Color.parseColor("#bbffffff")).apply { setPadding(0, dp(12), 0, dp(20)) })
        v.addView(Button(this).apply { text = "Закрыть"; setOnClickListener { stopSelf() } })
        wm.addView(v, lp(-1, -1, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
        revealView = v
    }

    // громкость не форсируем: звук идёт по обычной громкости медиа
    private fun playScream() {
        val pcm = synth()
        val at = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(22050).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
        at.write(pcm, 0, pcm.size); at.play(); track = at
    }

    private fun stopScream() {
        track?.let { try { it.stop() } catch (_: Exception) {}; it.release() }; track = null
    }

    private fun synth(): ShortArray {
        val sr = 22050; val n = (sr * SCARE_MS / 1000).toInt(); val out = ShortArray(n)
        var p1 = 0.0; var p2 = 0.0; val rnd = java.util.Random(7)
        for (i in 0 until n) {
            val t = i.toDouble() / sr; val bt = t % 1.2
            val env = when { bt < .03 -> bt / .03; bt < 1.0 -> 1.0; bt < 1.15 -> (1.15 - bt) / .15; else -> 0.0 }
            val base = if (bt < .45) 550.0 * (1500.0 / 550.0).pow(bt / .45)
                       else 1500.0 * (1100.0 / 1500.0).pow((min(bt, 1.1) - .45) / .65)
            val f = base * (1 + .045 * sin(2 * PI * 11 * t))
            p1 += 2 * PI * f / sr; p2 += 2 * PI * f * 2 / sr
            val saw = ((p1 / (2 * PI)) % 1.0) * 2 - 1
            val sq = if (sin(p2) >= 0) 1.0 else -1.0
            val s = (.55 * saw + .2 * sq + .2 * (rnd.nextDouble() * 2 - 1)) * env
            out[i] = (tanh(s * 3.0) * .9 * 32767).toInt().toShort()
        }
        return out
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        stopScream()
        for (v in listOf(hud, bubble, scareView, revealView)) v?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        if (menuShown) menuView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    // ---------- содержимое меню ----------
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun menuFor(sf: Boolean): LinkedHashMap<String, List<It>> {
        val m = LinkedHashMap<String, List<It>>()
        m["Визуал"] = listOf(
            Hd("ESP"),
            Tg("Box ESP", true) { hud?.esp = it },
            Tg("Углы вместо рамки") { hud?.corner = it },
            Tg("Скелет") { hud?.skel = it },
            Tg("Имена", true) { hud?.names = it },
            Tg("Дистанция", true) { hud?.dist = it },
            Tg("Полоска HP", true) { hud?.hp = it },
            Tg("Линии до врагов") { hud?.lines = it },
            Tg("Chams (подсветка моделей)"),
            Tg("Подсветка врагов"),
            Sl("Дальность ESP", 20, if (sf) 300 else 800, if (sf) 150 else 400, " м"),
            Cp("Цвет ESP", PALETTE[0]) { hud?.espCol = it },
            Hd("HUD"),
            Tg("Радар на экране", true) { hud?.radar = it },
            Tg("Счётчик врагов", true) { hud?.count = it },
            Tg("Прицел-крестик", true) { hud?.cross = it },
            Tg("Водяной знак (FPS/ping)", true) { hud?.wm = it },
            Hd(if (sf) "Мир" else "Объекты"),
            if (sf) Tg("Гранаты", true) else Tg("Транспорт"),
            if (sf) Tg("Дроп-лут", true) else Tg("Аирдроп", true),
            Tg("Оповещение о врагах", true))
        m["Аим"] = listOf(
            Hd("Aimbot"),
            Tg("Aimbot", true), Tg("Silent Aim", true), Tg("Авто на голову", true),
            Tg("Только видимые"), Tg("Игнорировать ботов", true), Tg("Предикт движения", true),
            Tg("Sticky target"), Tg(if (sf) "Triggerbot" else "Авто-стрельба"),
            Ch("Кость", listOf("Голова", "Шея", "Грудь", "Живот")),
            Ch("Приоритет цели", listOf("Ближайший", "Мин. HP", "К прицелу")),
            Ch("Активация", listOf("При стрельбе", "При прицеливании", "Всегда"), 1),
            Hd("FOV"),
            Tg("Рисовать FOV-круг", true) { hud?.fov = it },
            Sl("FOV", 5, 180, 60, "°") { hud?.fovV = it },
            Tg("Заливка круга") { hud?.fovFill = it },
            Cp("Цвет FOV", PALETTE[0]) { hud?.fovCol = it },
            Sl("Плавность", 1, 30, 8),
            Sl("Дальность аима", 10, 300, 120, " м"))
        m["Рейдж"] = listOf(
            Hd("Rage"),
            Tg("Rage Aimbot"), Tg("Авто-выстрел"), Tg("Auto Wall"), Tg("Magic Bullet"),
            Tg("Force Headshot"), Tg("Resolver"),
            Ch("Хитбокс", listOf("Голова", "Тело", "Все")),
            Sl("Rage FOV", 1, 360, 180, "°"), Sl("Hitchance", 1, 100, 85, "%"), Sl("Мин. урон", 1, 100, 30),
            Hd("Anti-Aim"),
            Tg("Anti-Aim"), Tg("Spinbot"), Tg("Fake Lag"), Tg("Double Tap"), Tg("Hide Shots"),
            Ch("Режим Anti-Aim", listOf("Jitter", "Spin", "Static", "Random")),
            Sl("Скорость вращения", 1, 100, 40), Sl("Fake Lag тики", 1, 24, 8))
        m["Движение"] = listOf(
            Hd("Перемещение"),
            Tg("BunnyHop", sf), Tg("Auto Strafe"), Tg("Скорость"), Tg("Быстрый бег"),
            Tg("Без урона от падения"), Tg("Прыжок в воздухе"), Tg("Без инерции"),
            Sl("Множитель скорости", 1, 3, 1, "x"), Sl("Высота прыжка", 1, 5, 1, "x"))
        m["Оружие"] = listOf(
            Hd("Отдача и разброс"),
            Tg("No Recoil", true), Tg("No Spread"), Tg("No Sway"),
            Hd("Стрельба"),
            Tg("Rapid Fire"), Tg("Бесконечные патроны"), Tg("Быстрая перезарядка"),
            Tg("Быстрая смена оружия"), Tg("Wallbang"), Tg("Авто-перезарядка"),
            Sl("Скорость стрельбы", 1, 5, 1, "x"))
        m["Лут"] = listOf(
            Hd("Лут"),
            Tg("Авто-лут", true), Tg("Быстрый подбор"), Tg("ESP предметов"), Tg("Фильтр: только топ"),
            Tg("Авто-экипировка"), Tg("Авто-аптечки"),
            Sl("Радиус лута", 5, 100, 40, " м"))
        m["Мисц"] = listOf(
            Hd("Игра"),
            Ch("FPS-анлок", listOf("60", "90", "120", "144"), 1),
            Tg("Широкий обзор"), Sl("FOV камеры", 60, 130, 90, "°"),
            Tg("Без тумана"), Tg(if (sf) "No Smoke" else "Без травы", true), Tg("No Flash"),
            Tg("Ночной режим"), Sl("Яркость", 0, 200, 100, "%"),
            Hd("Звук"),
            Tg("Hitsound", true), Tg("Killsound"), Tg("Анти-AFK", true))
        m["Защита"] = listOf(
            Hd("Защита аккаунта"),
            Tg("Anti-Ban", true), Tg("HWID Spoof", true), Tg("Streamproof", true),
            Tg("Скрыть от записи экрана", true), Tg("Маскировка процесса", true),
            Tg("Авто-очистка логов"), Tg("Блок репортов"))
        m["Скины"] = listOf(Sk(if (sf) linkedMapOf(
            "AK-47" to sk("Dragon:#fb923c", "Gold:#fde047", "Neon:#22d3ee", "Carbon:#52525b"),
            "M4" to sk("Howl:#f87171", "Hyper:#a78bfa", "Ice:#bae6fd", "Toxic:#4ade80"),
            "AWP" to sk("Lightning:#fde047", "Medusa:#34d399", "Asiimov:#f97316", "Void:#6366f1"),
            "Desert Eagle" to sk("Blaze:#fb923c", "Sunset:#f472b6", "Gold:#fde047", "Frost:#7dd3fc"),
            "Нож" to sk("Karambit Fade:#f472b6", "Butterfly:#94a3b8", "Bayonet Gold:#fde047", "M9 Doppler:#22d3ee"),
            "Перчатки" to sk("Спорт:#a78bfa", "Драйвер:#f87171", "Бомбер:#4ade80", "Хром:#e5e7eb"))
        else linkedMapOf(
            "AKM" to sk("Ледник:#7dd3fc", "Золотой дракон:#fde047", "Кровавая луна:#f87171", "Неон:#a78bfa"),
            "M416" to sk("Пламя:#fb923c", "Космос:#818cf8", "Яд:#4ade80", "Хром:#e5e7eb"),
            "AWM" to sk("Сакура:#f9a8d4", "Тень:#64748b", "Золото:#fde047", "Электро:#67e8f9"),
            "Kar98k" to sk("Дракон:#34d399", "Лава:#f97316", "Лёд:#bae6fd", "Оникс:#52525b"),
            "Костюм" to sk("Жнец:#a1a1aa", "Принц:#c4b5fd", "Огонь:#fb923c", "Золотой:#fde047"))))
        m["Консоль"] = listOf(Con)
        m["Конфиг"] = listOf(
            Ch("Слот", listOf("1", "2", "3", "4", "5")),
            Bt("Сохранить") { toast("Конфиг сохранён") },
            Bt("Загрузить") { toast("Конфиг загружен") },
            Bt("Сбросить") { toast("Настройки сброшены") },
            Bt("Экспорт") { toast("Скопировано") },
            Bt("Очистить следы") {
                listOf("Очистка логов...", "Сброс HWID...", "Следы очищены").forEachIndexed { i, s ->
                    h.postDelayed({ toast(s) }, i * 1300L)
                }
            })
        m["Настройки"] = listOf(
            Sl("Прозрачность меню", 30, 100, 90, "%") { menuView?.alpha = it / 100f },
            Cp("Цвет меню", accent) {
                accent = it; hud?.accent = it; accentLine?.setBackgroundColor(it)
                bubble?.background = ovalBg(it)
            },
            Tg("Водяной знак", true) { hud?.wm = it },
            Bt("Выйти из BURMALDA") { stopSelf() })
        return m
    }
}
