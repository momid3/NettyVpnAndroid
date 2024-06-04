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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.momid.momidvpn0.tcp.startSendConnection
import com.momid.momidvpn0.tcp.startReceivingConnection
import com.momid.momidvpn0.tcp.startSendingAndReceiving
import java.io.OutputStream

val SERVER_IP_ADDRESS = "141.98.210.95"

class MomidVpnService : VpnService() {

    private var receiveThread: Thread? = null
    private val binder: IBinder = ServiceBinder()
    private val serverSocketAddress = InetSocketAddress("141.98.210.95", 33338)
    private val executor = Executors.newSingleThreadExecutor()
    private val transmissionExecutor = Executors.newSingleThreadExecutor()
    var tcpOutputStream: OutputStream? = null


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

    public fun connect() {


        updateForegroundNotification("Connected to Momid Vpn")

        connected = CONNECTED

        connectionLiveData.value = CONNECTED
//        prepare(this)
//        if (udpHandler == null) {
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            startSendConnection()
//        }

//        Thread {
//            startSendConnection()
//        }.start()

//        CoroutineScope(Dispatchers.IO).launch {
//            startReceivingConnection()
//        }

//        Thread {
//            startReceivingConnection()
//        }.start()

        Thread {
            tcpOutputStream = startSendingAndReceiving {
                try {
                    if (output != null) {
                        println(it.joinToString(" ") { eachByte -> "%02x".format(eachByte) } + "\n\n\n")
                        output!!.write(it)
                    } else {
                        println("output is null")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }.start()

//        Thread {
//            while (true) {
////                runBlocking {
////                    launch {
//                        try {
//                            val packet = receivePackets.take()
//                            output?.write(packet) ?: println("output is null")
//                        } catch (t: Throwable) {
//                            t.printStackTrace()
//                        }
////                    }
////                }
//            }
//        }.start()

        println("starting")

//                        if (!protect()) {
//                            println("protecting socket failed")
//                            return@init
//                        }

//                        if (!protect(downloadProtocolClient!!.socket)) {
//                            println("protecting socket failed")
//                            return@init
//                        }

        parcelFileDescriptor =
            connectVpn() ?: kotlin.run { println("cannot connect"); return }
        try {
//            input = FileInputStream(parcelFileDescriptor.fileDescriptor)
            output = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            return
        }
        receiveThread =
            Thread {
                input =
                    FileInputStream(parcelFileDescriptor!!.fileDescriptor)
//                                    is_new_send.set(true)

                while (connected == CONNECTED) {
                    try {
                        val incomingDataSize = input!!.read(receiveBuffer)
                        executor.execute {
                            val incomingData =
                                receiveBuffer.sliceArray(0 until incomingDataSize)
//                                                    if (is_new_send.get()) {
//                                                        is_new_send.set(false)
//                                                        receiveBuffer.sliceArray(0 until incomingDataSize)
//                                                            .hide()
//                                                    } else {
//                                                        receiveBuffer.sliceArray(0 until incomingDataSize)
//                                                    }
//                            runBlocking {
//                                sendPackets.put(incomingData)
                            tcpOutputStream?.write(incomingData)
//                            }
                        }
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                        return@Thread
                    }
                }
            }

//                        receiveThread?.start()

//        udpHandler!!.startReceiving(object : UdpHandler.PacketListener {
//            override fun onPacket(packet: ByteArray, socketAddress: SocketAddress) {
//                try {
//                    output!!.write(packet)
////                    println("received" + packet.joinToString(" ") { String.format("0x%02X", it) })
//                } catch (throwable: Throwable) {
//                    throwable.printStackTrace()
//                }
//            }
//        })

        receiveThread?.start()
    }

    public fun disconnect() {

        connected = DISCONNECTED
        connectionLiveData.value = DISCONNECTED
        receiveThread?.interrupt()

        parcelFileDescriptor!!.close()

        stopForeground(true)
    }

    private fun connectVpn(): ParcelFileDescriptor? {
//        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//        val networks = connectivityManager.activeNetwork
        val vpnBuilder = this.Builder()
        vpnBuilder.addAddress("192.168.3.8", 32)
        vpnBuilder.addRoute("0.0.0.0", 0)
        vpnBuilder.addDnsServer("1.1.1.1")
        vpnBuilder.setMtu(1100)

//        vpnBuilder.addRoute("0.0.0.0", 1);
//        vpnBuilder.addRoute("128.0.0.0", 1);
//        vpnBuilder.addRoute("64.0.0.0", 2);
//        vpnBuilder.addRoute("192.0.0.0", 2);
//        vpnBuilder.addRoute("32.0.0.0", 3);
//        vpnBuilder.addRoute("96.0.0.0", 3);
//        vpnBuilder.addRoute("160.0.0.0", 3);
//        vpnBuilder.addRoute("224.0.0.0", 3);
//        vpnBuilder.addRoute("16.0.0.0", 4);
//        vpnBuilder.addRoute("48.0.0.0", 4);
//        vpnBuilder.addRoute("80.0.0.0", 4);
//        vpnBuilder.addRoute("112.0.0.0", 4);
//        vpnBuilder.addRoute("144.0.0.0", 4);
//        vpnBuilder.addRoute("176.0.0.0", 4);
//        vpnBuilder.addRoute("208.0.0.0", 4);
//        vpnBuilder.addRoute("8.0.0.0", 5);
//        vpnBuilder.addRoute("24.0.0.0", 5);
//        vpnBuilder.addRoute("40.0.0.0", 5);
//        vpnBuilder.addRoute("56.0.0.0", 5);
//        vpnBuilder.addRoute("72.0.0.0", 5);
//        vpnBuilder.addRoute("88.0.0.0", 5);
//        vpnBuilder.addRoute("104.0.0.0", 5);
//        vpnBuilder.addRoute("120.0.0.0", 5);
//        vpnBuilder.addRoute("136.0.0.0", 5);
//        vpnBuilder.addRoute("152.0.0.0", 5);
//        vpnBuilder.addRoute("168.0.0.0", 5);
//        vpnBuilder.addRoute("184.0.0.0", 5);
//        vpnBuilder.addRoute("200.0.0.0", 5);
//        vpnBuilder.addRoute("216.0.0.0", 5);
//        vpnBuilder.addRoute("232.0.0.0", 5);
//        vpnBuilder.addRoute("4.0.0.0", 6);
//        vpnBuilder.addRoute("12.0.0.0", 6);
//        vpnBuilder.addRoute("20.0.0.0", 6);
//        vpnBuilder.addRoute("28.0.0.0", 6);
//        vpnBuilder.addRoute("36.0.0.0", 6);
//        vpnBuilder.addRoute("44.0.0.0", 6);
//        vpnBuilder.addRoute("52.0.0.0", 6);
//        vpnBuilder.addRoute("60.0.0.0", 6);
//        vpnBuilder.addRoute("68.0.0.0", 6);
//        vpnBuilder.addRoute("76.0.0.0", 6);
//        vpnBuilder.addRoute("84.0.0.0", 6);
//        vpnBuilder.addRoute("92.0.0.0", 6);
//        vpnBuilder.addRoute("100.0.0.0", 6);
//        vpnBuilder.addRoute("108.0.0.0", 6);
//        vpnBuilder.addRoute("116.0.0.0", 6);
//        vpnBuilder.addRoute("124.0.0.0", 6);
//        vpnBuilder.addRoute("132.0.0.0", 6);
//        vpnBuilder.addRoute("140.0.0.0", 6);
//        vpnBuilder.addRoute("148.0.0.0", 6);
//        vpnBuilder.addRoute("156.0.0.0", 6);
//        vpnBuilder.addRoute("164.0.0.0", 6);
//        vpnBuilder.addRoute("172.0.0.0", 6);
//        vpnBuilder.addRoute("180.0.0.0", 6);
//        vpnBuilder.addRoute("188.0.0.0", 6);
//        vpnBuilder.addRoute("196.0.0.0", 6);
//        vpnBuilder.addRoute("204.0.0.0", 6);
//        vpnBuilder.addRoute("212.0.0.0", 6);
//        vpnBuilder.addRoute("220.0.0.0", 6);
//        vpnBuilder.addRoute("228.0.0.0", 6);
//        vpnBuilder.addRoute("236.0.0.0", 6);
//        vpnBuilder.addRoute("6.0.0.0", 7);
//        vpnBuilder.addRoute("14.0.0.0", 7);
//        vpnBuilder.addRoute("22.0.0.0", 7);
//        vpnBuilder.addRoute("30.0.0.0", 7);
//        vpnBuilder.addRoute("38.0.0.0", 7);
//        vpnBuilder.addRoute("46.0.0.0", 7);
//        vpnBuilder.addRoute("54.0.0.0", 7);
//        vpnBuilder.addRoute("62.0.0.0", 7);
//        vpnBuilder.addRoute("70.0.0.0", 7);
//        vpnBuilder.addRoute("78.0.0.0", 7);
//        vpnBuilder.addRoute("86.0.0.0", 7);
//        vpnBuilder.addRoute("94.0.0.0", 7);
//        vpnBuilder.addRoute("102.0.0.0", 7);
//        vpnBuilder.addRoute("110.0.0.0", 7);
//        vpnBuilder.addRoute("118.0.0.0", 7);
//        vpnBuilder.addRoute("126.0.0.0", 7);
//        vpnBuilder.addRoute("134.0.0.0", 7);
//        vpnBuilder.addRoute("142.0.0.0", 7);
//        vpnBuilder.addRoute("150.0.0.0", 7);
//        vpnBuilder.addRoute("158.0.0.0", 7);
//        vpnBuilder.addRoute("166.0.0.0", 7);
//        vpnBuilder.addRoute("174.0.0.0", 7);
//        vpnBuilder.addRoute("182.0.0.0", 7);
//        vpnBuilder.addRoute("190.0.0.0", 7);
//        vpnBuilder.addRoute("198.0.0.0", 7);
//        vpnBuilder.addRoute("206.0.0.0", 7);
//        vpnBuilder.addRoute("214.0.0.0", 7);
//        vpnBuilder.addRoute("222.0.0.0", 7);
//        vpnBuilder.addRoute("230.0.0.0", 7);
//        vpnBuilder.addRoute("238.0.0.0", 7);
//        vpnBuilder.addRoute("3.0.0.0", 8);
//        vpnBuilder.addRoute("7.0.0.0", 8);
//        vpnBuilder.addRoute("11.0.0.0", 8);
//        vpnBuilder.addRoute("15.0.0.0", 8);
//        vpnBuilder.addRoute("19.0.0.0", 8);
//        vpnBuilder.addRoute("23.0.0.0", 8);
//        vpnBuilder.addRoute("27.0.0.0", 8);
//        vpnBuilder.addRoute("31.0.0.0", 8);
//        vpnBuilder.addRoute("35.0.0.0", 8);
//        vpnBuilder.addRoute("39.0.0.0", 8);
//        vpnBuilder.addRoute("43.0.0.0", 8);
//        vpnBuilder.addRoute("47.0.0.0", 8);
//        vpnBuilder.addRoute("51.0.0.0", 8);
//        vpnBuilder.addRoute("55.0.0.0", 8);
//        vpnBuilder.addRoute("59.0.0.0", 8);
//        vpnBuilder.addRoute("63.0.0.0", 8);
//        vpnBuilder.addRoute("67.0.0.0", 8);
//        vpnBuilder.addRoute("71.0.0.0", 8);
//        vpnBuilder.addRoute("75.0.0.0", 8);
//        vpnBuilder.addRoute("79.0.0.0", 8);
//        vpnBuilder.addRoute("83.0.0.0", 8);
//        vpnBuilder.addRoute("87.0.0.0", 8);
//        vpnBuilder.addRoute("91.0.0.0", 8);
//        vpnBuilder.addRoute("95.0.0.0", 8);
//        vpnBuilder.addRoute("99.0.0.0", 8);
//        vpnBuilder.addRoute("103.0.0.0", 8);
//        vpnBuilder.addRoute("107.0.0.0", 8);
//        vpnBuilder.addRoute("111.0.0.0", 8);
//        vpnBuilder.addRoute("115.0.0.0", 8);
//        vpnBuilder.addRoute("119.0.0.0", 8);
//        vpnBuilder.addRoute("123.0.0.0", 8);
////        vpnBuilder.addRoute("127.0.0.0", 8);
//        vpnBuilder.addRoute("131.0.0.0", 8);
//        vpnBuilder.addRoute("135.0.0.0", 8);
//        vpnBuilder.addRoute("139.0.0.0", 8);
//        vpnBuilder.addRoute("143.0.0.0", 8);
//        vpnBuilder.addRoute("147.0.0.0", 8);
//        vpnBuilder.addRoute("151.0.0.0", 8);
//        vpnBuilder.addRoute("155.0.0.0", 8);
//        vpnBuilder.addRoute("159.0.0.0", 8);
//        vpnBuilder.addRoute("163.0.0.0", 8);
//        vpnBuilder.addRoute("167.0.0.0", 8);
//        vpnBuilder.addRoute("171.0.0.0", 8);
//        vpnBuilder.addRoute("175.0.0.0", 8);
//        vpnBuilder.addRoute("179.0.0.0", 8);
//        vpnBuilder.addRoute("183.0.0.0", 8);
//        vpnBuilder.addRoute("187.0.0.0", 8);
//        vpnBuilder.addRoute("191.0.0.0", 8);
//        vpnBuilder.addRoute("195.0.0.0", 8);
//        vpnBuilder.addRoute("199.0.0.0", 8);
//        vpnBuilder.addRoute("203.0.0.0", 8);
//        vpnBuilder.addRoute("207.0.0.0", 8);
//        vpnBuilder.addRoute("211.0.0.0", 8);
//        vpnBuilder.addRoute("215.0.0.0", 8);
//        vpnBuilder.addRoute("219.0.0.0", 8);
//        vpnBuilder.addRoute("223.0.0.0", 8);
//        vpnBuilder.addRoute("227.0.0.0", 8);
//        vpnBuilder.addRoute("231.0.0.0", 8);
//        vpnBuilder.addRoute("235.0.0.0", 8);
//        vpnBuilder.addRoute("239.0.0.0", 8);
//        vpnBuilder.addRoute("243.0.0.0", 8);
//        vpnBuilder.addRoute("247.0.0.0", 8);
//        vpnBuilder.addRoute("251.0.0.0", 8);
//        vpnBuilder.addRoute("255.0.0.0", 8);

        vpnBuilder.setBlocking(true)
        vpnBuilder.setSession("aoi")
        vpnBuilder.addDisallowedApplication("com.momid.momidvpn0")
//        vpnBuilder.setUnderlyingNetworks(arrayOf(networks))
        return vpnBuilder.establish()
    }


    public fun getConnectionLivedata(): LiveData<String> {
        return connectionLiveData
    }

    companion object {
        public var CONNECTED = "connected"
        public var DISCONNECTED = "disconnected"
        public var CONNECTING = "connecting"
    }

    public inner class ServiceBinder : Binder() {
        public fun getVpnService(): MomidVpnService {
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
