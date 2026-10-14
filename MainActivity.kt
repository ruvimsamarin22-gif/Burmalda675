package com.example.burmalda

import android.animation.ObjectAnimator
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.math.min
import kotlin.random.Random

// хеши ключей (как в HTML-версии)
private val LOGIN = longArrayOf(7504032099831383L,3144547274981819L,3993959324256553L,5703028121174795L,3959175879146431L,4845889958489579L,1103650451986766L,4333246287690292L,5263453694266786L,2142699815809753L,3927617593803856L,4980740164724363L,5318017429813371L,6839172941466071L,479909952032581L,5235520341295345L,3993304451310051L,2322202065045060L,2575908548516827L,2475304322961610L,2561309229309748L,860876450928091L,462731734742063L,70261950471144L,5637709592131773L,4942795329614663L,7039249889831488L,2902000597362435L,2239942516015610L,2349980424781391L,7779740697780957L,5799277908604034L,4497247206755848L,1330713160317384L,8419505582856750L,2540590942265004L,848612370520326L,4813918264803492L,5565934941282710L,7080331095090800L,1241068887061710L,1240671800825689L,2931114042453426L,5269120817908122L,3853640228601188L,3550362699186218L,1802863193686810L,1314529986381405L,7186621947853566L,1309092202842620L,5097001887315325L,131179715785245L,5417876276948661L,3686837829723341L,6984900689754233L,5537215530432561L,5409618394833269L,8973988367175891L,3339010892266316L,3872709909204222L,4815796457181528L,8791113959346933L,7283915719667543L,4453673685733448L,2171758793034986L,1609869993332212L,2464813407651148L,3875604667169152L,4040455174971510L,8010024793854481L,4578675066661737L,7697451822580496L,2702696310761485L,7928855317655755L,2903432002906575L,7641727022463296L,3529476309550019L,2626218469221954L,5083901599517237L,1517788474736395L,4345446940488083L,4886244574953821L,8462969445753057L,800763748722096L,5924067938428550L,4814246487002880L,5032199590912221L,6380547077395486L,7760380567957404L,2959804045749262L,308946084388019L,4680996585484830L,7761046395677551L,366573015696661L,5841835939262586L,1260101250961213L,5285047688104260L,5055125539572228L,4545718433883136L,8235331215941152L,4307473631046617L,7228677641700431L,4095733141782720L,144229507923421L,4072736189994327L,934718652129927L,5393619365085717L,2183059656801421L,4702122637271709L,3859337113160277L,2520321017604989L)

private fun h53(s: String): Long {
    var h1 = 0xdeadbeefL.toInt(); var h2 = 0x41c6ce57
    for (ch in s) {
        val k = ch.code
        h1 = (h1 xor k) * 2654435761L.toInt(); h2 = (h2 xor k) * 1597334677
    }
    h1 = ((h1 xor (h1 ushr 16)) * 2246822507L.toInt()) xor ((h2 xor (h2 ushr 13)) * 3266489909L.toInt())
    h2 = ((h2 xor (h2 ushr 16)) * 2246822507L.toInt()) xor ((h1 xor (h1 ushr 13)) * 3266489909L.toInt())
    return 4294967296L * (h2 and 2097151) + (h1.toLong() and 0xFFFFFFFFL)
}

class MainActivity : Activity() {
    private class G(val name: String, val sub: String, val pkg: String, val col: Int)

    private val orange = Color.parseColor("#f97316")
    private val green = Color.parseColor("#4ade80")
    private val t1 = Color.parseColor("#eef0ff")
    private val t2 = Color.parseColor("#8CEEF0FF")
    private val t3 = Color.parseColor("#40EEF0FF")
    private val games by lazy {
        listOf(
            G("Standoff 2", "Mobile", "com.axlebolt.standoff2", orange),
            G("PUBG Mobile", "KR", "com.pubg.krmobile", green),
            G("PUBG Mobile", "Global", "com.tencent.ig", green),
            G("PUBG Lite", "Lite", "com.tencent.iglite", green)
        )
    }
    private val h = Handler(Looper.getMainLooper())
    private var sel = 0
    private var cardW = 0
    private var pingTv: TextView? = null
    private lateinit var box: LinearLayout
    private val pingRun = object : Runnable {
        override fun run() { pingTv?.text = "${14 + Random.nextInt(34)} мс"; h.postDelayed(this, 1800) }
    }

    // ---------- помощники оформления ----------
    private fun c(s: String) = Color.parseColor(s)
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun rr(fill: Int, r: Int, stroke: Int = 0, sw: Int = 1) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(sw), stroke)
    }
    private fun grad(a: String, b: String, r: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c(a), c(b)))
            .apply { cornerRadius = dp(r).toFloat() }
    private fun tv(t: String, sp: Float, col: Int = t1, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = sp; setTextColor(col); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun btn(t: String, bg: Drawable, f: () -> Unit) = tv(t, 15f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER; setPadding(dp(16), dp(14), dp(16), dp(14)); background = bg
        setOnClickListener { f() }
    }
    private fun lpm(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    private fun newCard(): LinearLayout {
        h.removeCallbacks(pingRun); pingTv = null
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            background = rr(c("#0FFFFFFF"), 24, c("#1FFFFFFF"))
        }
    }
    private fun showCard(card: View) {
        box.removeAllViews(); box.addView(card, LinearLayout.LayoutParams(cardW, -2))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        cardW = min(resources.displayMetrics.widthPixels - dp(40), dp(390))
        val root = FrameLayout(this).apply { setBackgroundColor(c("#07081a")) }
        fun blob(c1: String, c2: String, gr: Int, x: Int, y: Int) {
            root.addView(View(this).apply {
                background = GradientDrawable().apply {
                    gradientType = GradientDrawable.RADIAL_GRADIENT
                    gradientRadius = dp(260).toFloat()
                    colors = intArrayOf(c(c1), c(c2))
                }
                translationX = dp(x).toFloat(); translationY = dp(y).toFloat()
            }, FrameLayout.LayoutParams(dp(520), dp(520), gr))
        }
        blob("#664c1d95", "#004c1d95", Gravity.TOP or Gravity.START, -200, -200)
        blob("#660c4a6e", "#000c4a6e", Gravity.BOTTOM or Gravity.END, 200, 200)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        root.addView(ScrollView(this).apply {
            isFillViewport = true; addView(box, FrameLayout.LayoutParams(-1, -2))
        })
        setContentView(root)
        screenLogin()
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    // ---------- экран 1: ключ ----------
    private fun screenLogin() {
        val card = newCard()
        card.addView(tv("BUR\nMALDA", 46f, bold = true).apply {
            setLineSpacing(0f, .92f)
            post {
                paint.shader = LinearGradient(0f, 0f, paint.measureText("MALDA"), 0f,
                    intArrayOf(c("#c4b5fd"), c("#22d3ee")), null, Shader.TileMode.CLAMP)
                invalidate()
            }
        }, lpm())
        card.addView(tv("Введи ключ доступа", 14f, t2), lpm(8))
        card.addView(tv("Ключ доступа", 12f, t2, true), lpm(20))
        val et = EditText(this).apply {
            hint = "BM//..."; setHintTextColor(t3); setTextColor(t1)
            typeface = Typeface.MONOSPACE; textSize = 16f; setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            background = rr(c("#66000000"), 13, c("#1FFFFFFF"))
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        val err = tv("", 12f, c("#f87171"))
        card.addView(et, lpm(6)); card.addView(err, lpm(6))
        card.addView(btn("Войти", grad("#7c3aed", "#0891b2", 13)) {
            if (LOGIN.contains(h53(et.text.toString().trim().uppercase()))) screenStatus()
            else {
                err.text = "Неверный ключ. Проверь символы и регистр."
                ObjectAnimator.ofFloat(card, "translationX", 0f, -24f, 24f, -24f, 24f, 0f).setDuration(400).start()
            }
        }, lpm(12))
        card.addView(tv("нет ключа?", 12f, t3).apply { gravity = Gravity.CENTER }, lpm(14))
        card.addView(btn("Получить ключ у @koloippol", grad("#2aabee", "#229ed9", 13)) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/koloippol")))
        }, lpm(8))
        showCard(card)
    }

    // ---------- экран 2: клиент активен ----------
    private fun screenStatus() {
        val card = newCard()
        card.addView(tv("Клиент активен", 20f, bold = true), lpm())
        card.addView(tv("Выбери игру и запусти", 14f, t2), lpm(6))
        val st = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rr(c("#47000000"), 14, c("#1FFFFFFF")); setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        fun row(k: String, v: String, col: Int): TextView {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6)) }
            r.addView(tv(k, 13.5f, t2), LinearLayout.LayoutParams(0, -2, 1f))
            val vt = tv(v, 13.5f, col).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
            r.addView(vt); st.addView(r); return vt
        }
        row("Сервер", "в сети", green); row("Лицензия", "активна", green)
        val ping = row("Пинг", "23 мс", t1)
        card.addView(st, lpm(18))

        val cards = ArrayList<LinearLayout>()
        fun paintCards() {
            cards.forEachIndexed { i, v ->
                val g = games[i]
                v.background = if (i == sel) rr((g.col and 0xFFFFFF) or (0x1F shl 24), 16, g.col, 2)
                               else rr(c("#47000000"), 16, c("#1FFFFFFF"), 2)
            }
        }
        for (r in 0..1) {
            val rowL = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in 0..1) {
                val i = r * 2 + k; val g = games[i]
                val cv = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(14), dp(10), dp(14))
                    addView(tv(g.name, 14.5f, bold = true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
                    addView(tv(g.sub, 12f, t2).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
                    setOnClickListener { sel = i; paintCards() }
                }
                cards.add(cv)
                rowL.addView(cv, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (k == 0) rightMargin = dp(5) else leftMargin = dp(5)
                })
            }
            card.addView(rowL, lpm(10))
        }
        paintCards()
        card.addView(btn("Запустить", grad("#7c3aed", "#0891b2", 13)) { onStart() }, lpm(16))
        showCard(card)
        pingTv = ping; h.postDelayed(pingRun, 1800)
    }

    private fun onStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши «Поверх других приложений» и нажми Запустить снова", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (packageManager.getLaunchIntentForPackage(games[sel].pkg) == null) {
            Toast.makeText(this, "Игра не установлена: ${games[sel].name}", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Работа в фоне")
            .setMessage("Клиенту нужно работать в фоне, пока ты в игре.")
            .setPositiveButton("Разрешить") { _, _ -> inject() }
            .setNegativeButton("Не сейчас") { _, _ -> inject() }
            .show()
    }

    // ---------- экран 3: «инъекция» ----------
    private fun inject() {
        val g = games[sel]
        val card = newCard()
        val title = tv("Инъекция", 20f, bold = true)
        val sub = tv("Не закрывай страницу", 14f, t2)
        val log = tv("", 12.5f, t2).apply { typeface = Typeface.MONOSPACE; minHeight = dp(150) }
        val bw = cardW - dp(44)
        val track = FrameLayout(this).apply { background = rr(c("#1AFFFFFF"), 3) }
        val fill = View(this).apply { background = grad("#7c3aed", "#22d3ee", 3) }
        track.addView(fill, FrameLayout.LayoutParams(1, -1))
        val pct = tv("0%", 12.5f, t2, true).apply { gravity = Gravity.END }
        val open = btn("Открыть игру", grad("#7c3aed", "#0891b2", 13)) { openGame() }
            .apply { visibility = View.GONE }
        card.addView(title, lpm()); card.addView(sub, lpm(6)); card.addView(log, lpm(14))
        card.addView(track, LinearLayout.LayoutParams(-1, dp(6))); card.addView(pct, lpm(8)); card.addView(open, lpm(12))
        showCard(card)

        val steps = listOf("Поиск процесса ${g.pkg}", "Получение доступа к памяти", "Загрузка библиотек",
            "Расшифровка модуля", "Инъекция в процесс", "Создание оверлея")
        val done = ArrayList<String>()
        var i = 0
        fun render(cur: String?) {
            val sb = SpannableStringBuilder()
            done.forEach {
                val s = sb.length; sb.append("[ok] $it\n")
                sb.setSpan(ForegroundColorSpan(green), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (cur != null) sb.append("[..] $cur")
            log.text = sb
        }
        fun setP(p: Int) { fill.layoutParams = FrameLayout.LayoutParams(maxOf(1, bw * p / 100), -1); pct.text = "$p%" }
        fun next() {
            if (i > 0) done.add(steps[i - 1])
            if (i >= steps.size) {
                render(null); setP(100); title.text = "Готово"
                sub.text = "Нажми, чтобы открыть игру"; open.visibility = View.VISIBLE; return
            }
            render(steps[i]); setP(((i + 1) * 92) / steps.size); i++
            h.postDelayed({ next() }, 700L + Random.nextLong(700))
        }
        next()
    }

    private fun openGame() {
        val pkg = games[sel].pkg
        startForegroundService(Intent(this, OverlayService::class.java).putExtra(OverlayService.EXTRA_PKG, pkg))
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
