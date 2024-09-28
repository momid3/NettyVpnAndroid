package com.momid.momidvpn0

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.momid.channel
import com.momid.incomingInternetPackets
import com.momid.initClient
import com.momid.startClient
import io.netty.buffer.Unpooled
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

val SERVER_IP_ADDRESS = "141.98.210.95"

val retryLock = ReentrantLock()

val retry = retryLock.newCondition()

class MomidVpnService : VpnService() {

    private var receiveThread: Thread? = null
    private val binder: IBinder = ServiceBinder()

    private var input: FileInputStream? = null

    private var output: FileOutputStream? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    public var connected = DISCONNECTED

    private var connectionLiveData = MutableLiveData<String>(DISCONNECTED)

    private val receiveBuffer = ByteArray(Short.MAX_VALUE.toInt())

    private var ongoing = false

    private var initialized = false


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        connect()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun connect() {
        ongoing = true

        if (!initialized) {
            initClient({
                println("connected")
                Thread {
                    vpnHandShakeEstablished()
                }.start()
            }, {
                connected = CONNECTING
                connectionLiveData.postValue(CONNECTING)

                retryLock.withLock {
                    retry.signal()
                }
//                receiveThread?.interrupt()

//                parcelFileDescriptor!!.close()
            })
            initialized = true
        }

        Thread {
            while (ongoing) {
                retryLock.withLock {
                    connectWithIp()
                    retry.await()
                    Thread.sleep(3000)
                    println("retrying")
                    if (channel?.isActive ?: false) {
                        channel!!.close()
                    }
                }
            }
        }.start()
    }

    fun connectWithIp(): Boolean {
        updateForegroundNotification("Connected to Momid Vpn")

        connectionLiveData.postValue(CONNECTING)
        connected = CONNECTING
        ongoing = true

        Thread {
            startClient()
        }.start()

        println("starting")

        return !ongoing
    }

    fun vpnHandShakeEstablished() {
        connected = CONNECTED
        connectionLiveData.postValue(CONNECTED)

//            startConnection()

//            connectRandomPenetration()

        parcelFileDescriptor =
            connectVpn() ?: kotlin.run { println("cannot connect"); return }
        try {
            output = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            return
        }
        receiveThread = Thread {
            input = FileInputStream(parcelFileDescriptor!!.fileDescriptor)

            while (connected == CONNECTED) {
                if (connected == CONNECTED) {
                    try {
                        val incomingDataSize = input!!.read(receiveBuffer)
                        val incomingData = receiveBuffer.sliceArray(0 until incomingDataSize)
                        println("sending")
//                        println(incomingData.joinToString(" ") {
//                            it.toHexString()
//                        })
                        val packet = Unpooled.copiedBuffer(incomingData)
                        channel?.writeAndFlush(packet)?.addListener {
                            if (it.isSuccess) {
                                println("writing to channel")
                            }
                            if (!it.isSuccess) {
                                println("is not sent")
                                it.cause().printStackTrace()
                            }
                        }
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                    }
                }
            }
        }

        receiveThread?.start()

        while (connected == CONNECTED) {
            if (connected == CONNECTED) {
                try {
                    if (output != null) {
                        val packet = incomingInternetPackets.take()
                        println("received " + packet.size)
//                        println(packet.joinToString(" ") {
//                            it.toHexString()
//                        })
//                            println(it.joinToString(" ") { eachByte -> "%02x".format(eachByte) } + "\n\n\n")
                        output!!.write(packet)
                    } else {
                        println("output is null")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    fun disconnect() {

        connected = DISCONNECTED
        connectionLiveData.value = DISCONNECTED
        ongoing = false
        receiveThread?.interrupt()

        parcelFileDescriptor!!.close()

        stopForeground(true)
    }

    private fun connectVpn(): ParcelFileDescriptor? {
        val vpnBuilder = this.Builder()
        val address = InetAddress.getByAddress(currentLocalIp)
        vpnBuilder.addAddress(address, 24)
        vpnBuilder.addRoute("0.0.0.0", 0)
        vpnBuilder.addDnsServer("8.8.8.8")
        vpnBuilder.setMtu(1380)

        vpnBuilder.setBlocking(true)
        vpnBuilder.setSession("aoi")
        vpnBuilder.addDisallowedApplication("com.momid.momidvpn0")
        return vpnBuilder.establish()
    }


    fun getConnectionLivedata(): LiveData<String> {
        return connectionLiveData
    }

    companion object {
        var CONNECTED = "connected"
        var DISCONNECTED = "disconnected"
        var CONNECTING = "connecting"
    }

    inner class ServiceBinder : Binder() {
        fun getVpnService(): MomidVpnService {
            return this@MomidVpnService
        }
    }

    private fun updateForegroundNotification(contentText: String) {
        val NOTIFICATION_CHANNEL_ID = "aoi"
        val mNotificationManager = getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(
                3, Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText(contentText)
                    //                .setContentIntent()
                    .build()
            )
        }
    }

//    fun startConnection() {
//        val client = startClient {
//            if (ongoing) {
//                println("reconnecting...")
//                connectionLiveData.postValue(CONNECTING)
//                connected = CONNECTING
//                Thread.sleep(3000)
//                startConnection()
//            }
//        }
//
//        if (client) {
//            println("connected")
//
//            connected = CONNECTED
//
//            connectionLiveData.postValue(CONNECTED)
//        } else {
//            if (ongoing) {
//                println("reconnecting...")
//                connectionLiveData.postValue(CONNECTING)
//                connected = CONNECTING
//                Thread.sleep(3000)
//                startConnection()
//            }
//        }
//    }

    fun connectRandomPenetration() {
        // List of random real endpoints
        val urls = listOf(
            "https://www.google.com",          // Google search
            "https://www.wikipedia.org",       // Wikipedia homepage
            "https://www.googleapis.com",
//            "https://www.twitter.com",         // Twitter homepage
            "https://play.googleapis.com",
            "https://www.gstatic.com",
//            "https://www.reddit.com",          // Reddit homepage
            "https://fcm.googleapis.com",
//            "https://www.nytimes.com",         // New York Times
            "https://www.github.com",          // GitHub
            "https://www.stackoverflow.com",   // StackOverflow homepage
//            "https://www.ebay.com",            // eBay homepage
            "https://notifications.google.com",
//            "https://www.spotify.com",         // Spotify homepage
//            "https://www.tumblr.com",          // Tumblr homepage
            "https://www.googleapis.com/youtube/v3"
        )
        repeat(3) {
            Thread {
                Thread.sleep(Random.nextInt(0, 3000).toLong())
                while (ongoing) {
                    if (connected == CONNECTED) {
                        // Create OkHttpClient instance
                        val client = OkHttpClient()

//                        for (url in urls) {
                        val url = urls.random()
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .build()

                            // Execute the request
                            val response: Response = client.newCall(request).execute()

                            // Print the status code and response body
                            println("Connecting to: $url")
                            println("Response Code: ${response.code}")
                            println(
                                "Response Body: ${
                                    response.body?.string()?.take(200)
                                }..."
                            ) // Truncated for readability
                            println("---------------")

                        } catch (e: IOException) {
                            println("Failed to connect to $url")
                            e.printStackTrace()
                        }

                        Thread.sleep(3000)
//                        }
                    }
                }
            }.start()
        }
    }
}
