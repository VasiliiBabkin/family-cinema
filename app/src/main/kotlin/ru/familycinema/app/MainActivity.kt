package ru.familycinema.app

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.familycinema.domain.Film
import ru.familycinema.parental.FamilyStore
import ru.familycinema.player.Playback
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
 override fun attachBaseContext(base: Context) {
  val config=Configuration(base.resources.configuration).apply { setLocale(java.util.Locale("ru")) }
  super.attachBaseContext(base.createConfigurationContext(config))
 }
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val tv = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
  setContent { Cinema(tv) }
 }
}
private val Ink=Color(0xFF17233B)
private val Gold=Color(0xFFF5C678)

@Composable private fun Cinema(tv: Boolean) {
 val context=LocalContext.current
 val store=remember { FamilyStore(context) }
 var screen by remember { mutableStateOf(if(store.hasPin) "catalog" else "pin") }
 var parent by remember { mutableStateOf(false) }
 var revision by remember { mutableIntStateOf(0) }
 val sessionOwner=LocalLifecycleOwner.current
 DisposableEffect(sessionOwner) {
  val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP && parent) {parent=false;screen="catalog"} }
  sessionOwner.lifecycle.addObserver(observer)
  onDispose {sessionOwner.lifecycle.removeObserver(observer)}
 }
 val film=remember { DemoCatalog.films().first() }
 val approved=remember(revision) { store.approved(film) }
 val dark=parent || screen=="pin" || screen=="player"
 val activity=context as? ComponentActivity
 DisposableEffect(screen,tv,activity) {
  if(screen=="player" && activity!=null) {
   if(!tv) activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
   WindowInsetsControllerCompat(activity.window,activity.window.decorView).apply {
    systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    hide(WindowInsetsCompat.Type.systemBars())
   }
  }
  onDispose {
   if(screen=="player" && activity!=null) {
    WindowInsetsControllerCompat(activity.window,activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
    if(!tv) activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
   }
  }
 }
 MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Gold,background=Ink,surface=Color(0xFF22334D)) else lightColorScheme(primary=Color(0xFF395648),background=Color(0xFFFFF5E5),surface=Color(0xFFFFE9C6))) {
  Surface(if(screen=="player") Modifier.fillMaxSize() else Modifier.fillMaxSize().safeDrawingPadding()) {
   BackHandler(screen!="catalog") { screen=if(screen=="player") "details" else "catalog" }
   if(screen=="player") {
    if(parent || approved) VideoScreen(film,store) { screen="details" } else LaunchedEffect(Unit) { screen="catalog" }
   } else BoxWithConstraints(Modifier.fillMaxSize()) {
    val wide=maxWidth>=840.dp
    val inset=if(tv) 48.dp else 24.dp
    if(tv || wide) Row(Modifier.fillMaxSize().padding(inset),horizontalArrangement=Arrangement.spacedBy(36.dp)) {
     Column(Modifier.width(220.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
      Text("СЕМЕЙНЫЙ\nКИНОТЕАТР",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text("Вечер вместе",color=MaterialTheme.colorScheme.primary)
      Action("Детский каталог") { parent=false; screen="catalog" }
      Action(if(parent) "Выйти из режима" else "Родителям") { if(parent) { parent=false;screen="catalog" } else screen="pin" }
      Text(if(tv) "Управление: стрелки, OK и Назад" else "Семейные истории под вашим контролем",style=MaterialTheme.typography.bodyMedium)
     }
     Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
      Content(screen,parent,approved,film,store,wide,{screen=it},{if(sessionOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {parent=true;screen="parent"}},{revision++})
     }
    } else Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(22.dp)) {
     Text("СЕМЕЙНЫЙ КИНОТЕАТР",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
     if(screen!="pin") Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      Action("Детям") { parent=false;screen="catalog" }
      Action(if(parent) "Выйти" else "Родителям") { if(parent) {parent=false;screen="catalog"} else screen="pin" }
     }
     Content(screen,parent,approved,film,store,wide,{screen=it},{if(sessionOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {parent=true;screen="parent"}},{revision++})
    }
   }
  }
 }
}

@Composable private fun Content(screen: String,parent: Boolean,approved: Boolean,film: Film,store: FamilyStore,wide: Boolean,navigate:(String)->Unit,unlocked:()->Unit,changed:()->Unit) {
 when(screen) {
  "pin" -> PinScreen(store,unlocked)
  "catalog" -> {
   Text("Маленькие истории.\nБольшие вечера.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
   Text("Твой уютный кинотеатр",style=MaterialTheme.typography.titleMedium)
   if(approved) FilmCard(film,wide) { navigate("details") } else {
    Text("Здесь появятся ваши истории",style=MaterialTheme.typography.headlineSmall)
    Text("Пока нет одобренных видео. Попроси родителя выбрать и проверить первую историю.")
   }
  }
  "parent" -> if(parent) {
   Text("Родительская",style=MaterialTheme.typography.headlineLarge)
   Text("Проверяйте каждую версию лично. Возрастная метка не гарантирует подходящее содержание.")
   FilmCard(film,wide) { navigate("details") }
   Text(if(approved) "Эта версия доступна ребёнку" else "Эта версия ещё не одобрена")
   Text("Сейчас работает только локальный каталог. Поиск, серверная синхронизация и BitTorrent появятся на следующих этапах.")
  } else LaunchedEffect(Unit) { navigate("catalog") }
  "details" -> if(parent || approved) {
   Poster(Modifier.fillMaxWidth().height(if(wide) 260.dp else 190.dp))
   Text(film.title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
   Text("${film.age}+  •  40 секунд  •  Оригинальный звук")
   Text(film.description)
   Text(film.license,style=MaterialTheme.typography.bodySmall)
   Action(if(store.position(film.id)>1000) "Продолжить просмотр" else "Смотреть") { navigate("player") }
   if(parent) {
    Text("Одобрение относится только к этой версии: ${film.version.id}")
    Action(if(approved) "Убрать из детского каталога" else "Одобрить эту версию") { store.approve(film,!approved);changed() }
   }
  } else LaunchedEffect(Unit) { navigate("catalog") }
 }
}
@Composable private fun Poster(modifier: Modifier) {
 Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF17233B),Color(0xFF365C50))),RoundedCornerShape(24.dp)).padding(24.dp)) {
  Text("ТИХИЙ\nВЕЧЕР",color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
  Text("ОРИГИНАЛЬНОЕ ТЕСТОВОЕ ВИДЕО",color=Gold,style=MaterialTheme.typography.labelMedium,modifier=Modifier.align(Alignment.BottomStart))
 }
}
@Composable private fun FilmCard(film: Film,wide: Boolean,onClick:()->Unit) {
 Column(Modifier.widthIn(max=if(wide) 560.dp else 700.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Poster(Modifier.fillMaxWidth().height(210.dp))
  Text(film.title,style=MaterialTheme.typography.titleLarge)
  Text("0+  •  Тестовый ролик  •  Без сети")
  Action("Открыть карточку",onClick)
 }
}
@Composable private fun Action(label: String,onClick:()->Unit) {
 var focused by remember { mutableStateOf(false) }
 val outline by animateColorAsState(if(focused) MaterialTheme.colorScheme.onSurface else Color.Transparent,label="Фокус")
 val scale by animateFloatAsState(if(focused) 1.04f else 1f,label="Масштаб фокуса")
 Button(onClick,modifier=Modifier.scale(scale).heightIn(min=52.dp).onFocusChanged { focused=it.isFocused }.border(if(focused) 4.dp else 0.dp,outline,RoundedCornerShape(28.dp)).padding(4.dp)) { Text(label) }
}
@Composable private fun PinScreen(store: FamilyStore,onSuccess:()->Unit) {
 var pin by remember { mutableStateOf("") }
 var confirmation by remember { mutableStateOf<String?>(null) }
 var message by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 val scope=rememberCoroutineScope()
 Text(if(store.hasPin) "Вход для родителей" else "Создайте родительский PIN",style=MaterialTheme.typography.headlineLarge)
 Text(if(store.hasPin) "Введите PIN с помощью кнопок ниже" else "Выберите 4–8 цифр. PIN хранится на этом устройстве; запомните его.")
 Text(if(pin.isEmpty()) "○ ○ ○ ○" else "● ".repeat(pin.length),style=MaterialTheme.typography.headlineMedium)
 if(confirmation!=null) Text("Повторите PIN для подтверждения")
 if(message.isNotEmpty()) Text(message,color=MaterialTheme.colorScheme.error)
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("Стереть","0","Очистить")).forEach { row ->
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { digit ->
    Action(digit) { if(!busy) { pin=when(digit) { "Стереть"->pin.dropLast(1);"Очистить"->"";else->if(pin.length<8) pin+digit else pin } } }
   } }
  }
 }
 Action(if(busy) "Проверяем…" else "Подтвердить") {
  if(!busy) {
   if(System.currentTimeMillis()<store.lockedUntil) { message="Слишком много попыток. Подождите минуту." }
   else if(pin.length<4) message="Нужно от 4 до 8 цифр"
   else if(!store.hasPin && confirmation==null) { confirmation=pin;pin="";message="" }
   else if(!store.hasPin && confirmation!=pin) { confirmation=null;pin="";message="PIN не совпали. Начните заново." }
   else { busy=true; val entered=pin;scope.launch {
    val success=withContext(Dispatchers.Default) { if(!store.hasPin) {store.setPin(entered);true} else store.verify(entered) }
    busy=false;pin=""
    if(success) { store.failedAttempts=0;store.lockedUntil=0;onSuccess() }
    else {store.failedAttempts++;if(store.failedAttempts>=5) {store.lockedUntil=System.currentTimeMillis()+60000;store.failedAttempts=0};message="Неверный PIN"}
   } }
  }
 }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable private fun VideoScreen(film: Film,store: FamilyStore,onBack:()->Unit) {
 val context=LocalContext.current
 var verified by remember { mutableStateOf<Boolean?>(null) }
 var error by remember { mutableStateOf("") }
 LaunchedEffect(film.version.fingerprint) {
  verified=withContext(Dispatchers.IO) { runCatching {
   val digest=MessageDigest.getInstance("SHA-256")
   context.assets.open("quiet-evening.mp4").use { input -> val bytes=ByteArray(8192);while(true) {val n=input.read(bytes);if(n<0) break;digest.update(bytes,0,n)} }
   digest.digest().joinToString("") { "%02x".format(it) }==film.version.sha256
  }.getOrDefault(false) }
 }
 if(verified!=true) Column(Modifier.padding(32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(if(verified==false) "Видео повреждено или изменилось. Просмотр заблокирован." else "Проверяем видео…")
  Action("К карточке",onBack)
 } else {
  var player by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
  val owner=LocalLifecycleOwner.current
  LaunchedEffect(film.id,owner) {
   withContext(Dispatchers.Main.immediate) {
   val activePlayer=Playback.create(context,film.version.uri,store.position(film.id))
   player=activePlayer
   val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) {store.savePosition(film.id,activePlayer.currentPosition);activePlayer.pause()} }
   val listener=object:Player.Listener {
    override fun onPlayerError(e: PlaybackException) { error="Не удалось воспроизвести видео. Вернитесь к карточке и повторите попытку." }
    override fun onPlaybackStateChanged(state:Int) { if(state==Player.STATE_ENDED) store.savePosition(film.id,0) }
   }
   activePlayer.addListener(listener);owner.lifecycle.addObserver(observer)
   try { awaitCancellation() }
   finally { store.savePosition(film.id,if(activePlayer.playbackState==Player.STATE_ENDED) 0 else activePlayer.currentPosition);owner.lifecycle.removeObserver(observer);activePlayer.removeListener(listener);activePlayer.release();player=null }
   }
  }
  val activePlayer=player
  Box(Modifier.fillMaxSize().background(Color.Black)) {
   AndroidView(factory={ PlayerView(it).apply {
    this.player=activePlayer
    useController=true
    controllerAutoShow=true
    controllerShowTimeoutMs=3500
    setShowSubtitleButton(false)
    resizeMode=androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
    requestFocus()
   } },modifier=Modifier.fillMaxSize(),update={it.player=activePlayer},onRelease={it.player=null})
   if(error.isNotEmpty()) Text(error,Modifier.align(Alignment.TopCenter).padding(24.dp),color=Color.White)
   TextButton(onClick=onBack,modifier=Modifier.align(Alignment.TopStart).padding(12.dp),colors=ButtonDefaults.textButtonColors(contentColor=Color.White)) { Text("‹ К фильму") }
  }
 }
}
