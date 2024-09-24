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
import com.momid.startClient
import io.netty.buffer.Unpooled
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

val SERVER_IP_ADDRESS = "141.98.210.95"

class MomidVpnService : VpnService() {

    private var receiveThread: Thread? = null
    private val binder: IBinder = ServiceBinder()

    private var input: FileInputStream? = null

    private var output: FileOutputStream? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    public var connected = DISCONNECTED

    private var connectionLiveData = MutableLiveData<String>(DISCONNECTED)

    private val receiveBuffer = ByteArray(Short.MAX_VALUE.toInt())


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        connect()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun connect() {
        updateForegroundNotification("Connected to Momid Vpn")

        connected = CONNECTED

        connectionLiveData.value = CONNECTED

        Thread {
            startClient()

            parcelFileDescriptor =
                connectVpn() ?: kotlin.run { println("cannot connect"); return@Thread }
            try {
                output = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                return@Thread
            }
            receiveThread = Thread {
                input = FileInputStream(parcelFileDescriptor!!.fileDescriptor)

                while (connected == CONNECTED) {
                    try {
                        val incomingDataSize = input!!.read(receiveBuffer)
                        val incomingData = receiveBuffer.sliceArray(0 until incomingDataSize)
                        println("sending")
                        println(incomingData.joinToString(" ") {
                            it.toHexString()
                        })
                        val packet = Unpooled.copiedBuffer(incomingData)
                        channel!!.writeAndFlush(packet)
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                        return@Thread
                    }
                }
            }

            receiveThread?.start()

            while (true) {
                try {
                    if (output != null) {
                        val packet = incomingInternetPackets.take()
                        println("received " + packet.size)
                        println(packet.joinToString(" ") {
                            it.toHexString()
                        })
//                            println(it.joinToString(" ") { eachByte -> "%02x".format(eachByte) } + "\n\n\n")
                        output!!.write(packet)
                    } else {
                        println("output is null")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }.start()

        println("starting")
    }

    fun disconnect() {

        connected = DISCONNECTED
        connectionLiveData.value = DISCONNECTED
        receiveThread?.interrupt()

        parcelFileDescriptor!!.close()

        stopForeground(true)
    }

    private fun connectVpn(): ParcelFileDescriptor? {
        val vpnBuilder = this.Builder()
        vpnBuilder.addAddress("10.0.0.3", 24)
        vpnBuilder.addRoute("0.0.0.0", 0)
        vpnBuilder.addDnsServer("8.8.8.8")
        vpnBuilder.setMtu(1300)

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
}
