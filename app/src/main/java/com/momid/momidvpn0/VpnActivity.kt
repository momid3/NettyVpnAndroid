package com.momid.momidvpn0

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class VpnActivity : AppCompatActivity() {

    private var momidVpnService : MomidVpnService? = null
    private lateinit var connectButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn)

        startService(Intent(this, MomidVpnService::class.java))
        bindService(Intent(this, MomidVpnService::class.java), object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {

                println("aoi")
                momidVpnService = binder?.let { (it as MomidVpnService.ServiceBinder).getVpnService() }

                if (momidVpnService != null) {
                    var connected = momidVpnService!!.connected

                    if (connected == MomidVpnService.CONNECTED) {

                        connectButton.text = "Connected"
                    }

                    if (connected == MomidVpnService.DISCONNECTED) {
                        connectButton.text = "Connect"
                    }

                    if (connected == MomidVpnService.CONNECTING) {
                        connectButton.text = "connecting..."
                    }
                }



                momidVpnService!!.getConnectionLivedata().observe(this@VpnActivity) {
                    it?.let {
                        if (it == MomidVpnService.CONNECTED) {

                            connectButton.text = "Connected"
                        }
                        if (it == MomidVpnService.DISCONNECTED) {
                            connectButton.text = "Connect"
                        }
                        if (it == MomidVpnService.CONNECTING) {
                            connectButton.text = "connecting..."
                        }
                    }
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                TODO("Not yet implemented")
            }

        }, Context.BIND_AUTO_CREATE)


        connectButton = findViewById(R.id.connect)


        connectButton.setOnClickListener {
            if (momidVpnService == null) {
                return@setOnClickListener
            }
            val connected = momidVpnService!!.connected
            if (connected == MomidVpnService.DISCONNECTED) {
                VpnService.prepare(this@VpnActivity)?.let {
                    startActivityForResult(it, 0)
                } ?: kotlin.run { momidVpnService!!.connect() }
            }

            if (connected == MomidVpnService.CONNECTED) {
                momidVpnService!!.disconnect()
            }

            if (connected == MomidVpnService.CONNECTING) {
                momidVpnService!!.disconnect()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        momidVpnService!!.connect()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (momidVpnService != null && momidVpnService!!.getConnectionLivedata().value == MomidVpnService.DISCONNECTED) {
            momidVpnService!!.stopSelf()
        }
    }
}
