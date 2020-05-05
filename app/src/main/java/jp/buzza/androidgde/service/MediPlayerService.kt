package jp.buzza.androidgde.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaSessionManager
import jp.buzza.androidgde.R
import timber.log.Timber
import java.io.IOException


class MediaPlayerService : Service(), OnPreparedListener, OnErrorListener,
    OnCompletionListener, OnSeekCompleteListener, OnInfoListener,
    OnBufferingUpdateListener, OnAudioFocusChangeListener {

    companion object {
        const val NOTIFICATION_ID = 101
    }

    private val urlStreaming = "http://mu1stream.mushost.com:8230/128kbps?type=http&nocache=30428"

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var mediaTransportControl: MediaControllerCompat.TransportControls
    private val musicBind = MusicBinder()
    private var resumePosition: Int = -1

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pause()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY
        mediaPlayer.start()
        return super.onStartCommand(intent, flags, startId)


    }

    override fun onBind(intent: Intent?): IBinder? {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mediaSessionCompat?.release()
        mediaPlayer.stop()
        mediaPlayer.release()
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
                Timber.d("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK")
            }
            MEDIA_ERROR_SERVER_DIED -> {
                Timber.d("MEDIA_ERROR_SERVER_DIED")
            }
            MEDIA_ERROR_UNKNOWN -> {
                Timber.d("MEDIA_ERROR_UNKNOWN")
            }
            else -> {
                Timber.d("ANOTHER ERROR !!!")
            }
        }
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mediaPlayer.reset()
        mediaPlayer.start()
    }

    override fun onSeekComplete(mp: MediaPlayer?) {
        // Do nothing
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        // Do nothing
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Do nothing
    }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
    }

    private fun start() {
        if (mediaPlayer.isPlaying.not()) {
            mediaPlayer.start()
        }
    }

    private fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    private fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            resumePosition = mediaPlayer.currentPosition
        }
    }

    private fun resume() {
        if (mediaPlayer.isPlaying.not()) {
            mediaPlayer.seekTo(resumePosition)
            mediaPlayer.start()
        }
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnBufferingUpdateListener(this)
        mediaPlayer.setOnSeekCompleteListener(this)
        mediaPlayer.setOnInfoListener(this)
        mediaPlayer.reset()

        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        try {
            mediaPlayer.setDataSource(urlStreaming)

        } catch (e: Exception) {
            when (e) {
                is IOException -> {
                }
                is IllegalArgumentException -> {
                }
                else -> {
                }
            }
            stopSelf()
        }
        mediaPlayer.prepareAsync()
    }

    @SuppressLint("ServiceCast")
    private fun initMediaSession() {
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionCompat = MediaSessionCompat(applicationContext, "TEST")
        mediaTransportControl = mediaSessionCompat.controller.transportControls
        mediaSessionCompat.isActive = true
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        updateMetaData()

        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                resume()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onPause() {
                super.onPause()
                pause()
                buildNotification(PlaybackStatus.PAUSED)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                stopSelf()
            }
        })
    }

    private fun removeNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private val metaRetriever = MediaMetadataRetriever().apply {
        setDataSource(urlStreaming)
    }

    private fun updateMetaData() {
        val artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        mediaSessionCompat.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build()
        )
    }

    /**
     * @param playbackAction
     * @return
     */
    private fun playbackAction(playbackAction: PlaybackAction): PendingIntent? {
        val intent = Intent(this, MediaPlayerService::class.java).apply {
            action = playbackAction.name
        }
        return PendingIntent.getService(this, playbackAction.raw, intent, 0)
    }

    /**
     * Notification actions -> playbackAction()
     * 0 -> Play
     * 1 -> Pause
     * 2 -> Next track
     * 3 -> Previous track
     *
     * @param playbackStatus
     */
    private fun buildNotification(playbackStatus: PlaybackStatus) {
        var notificationAction = android.R.drawable.ic_media_pause //needs to be initialized
        var pendingIntent: PendingIntent? = null

        //Build a new notification according to the current state of the MediaPlayer
        when (playbackStatus) {
            PlaybackStatus.PLAYING -> {
                notificationAction = android.R.drawable.ic_media_pause
                pendingIntent = playbackAction(PlaybackAction.PAUSE)
            }
            PlaybackStatus.PAUSED -> {
                notificationAction = android.R.drawable.ic_media_play
                pendingIntent = playbackAction(PlaybackAction.PLAY)
            }
        }
        val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.ic_launcher_foreground
        ) //replace with your own image

        // Create a new Notification
        val notificationBuilder = NotificationCompat.Builder(this, "channelID")
            .setShowWhen(false) // Set the Notification style
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(largeIcon))
            .setColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.colorAccent
                )
            ) // Set the large and small icons
            .setLargeIcon(largeIcon)
            .setSmallIcon(android.R.drawable.stat_sys_headset) // Set Notification content information
            .setContentText(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            .setContentTitle(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .addAction(notificationAction, "pause", pendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder)
    }


    inner class MusicBinder : Binder() {
        fun getService(): MediaPlayerService {
            return this@MediaPlayerService
        }
    }


}