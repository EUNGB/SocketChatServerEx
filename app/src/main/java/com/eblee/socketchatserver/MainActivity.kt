package com.eblee.socketchatserver

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {

    lateinit var serverSocket: ServerSocket
    lateinit var socket: Socket
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private val isConnected: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            tv_ip_address.text = getLocalIpAddress()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        btn_open.setOnClickListener {
            serverSocketOpen()
        }

        btn_send.setOnClickListener {
            sendMessage()
            et_message.setText("")
        }
    }


    @SuppressLint("SetTextI18n")
    @Throws(IOException::class)
    private fun serverSocketOpen() {
        val port = et_port.text.toString()
        if (port.isEmpty()) {
            Toast.makeText(this, "포트번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
        } else {
            Thread {
                try {
                    //서버소켓 생성.
                    serverSocket = ServerSocket(port.toInt())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    //서버에 접속하는 클라이언트 소켓 얻어오기(클라이언트가 접속하면 클라이언트 소켓 리턴)
                    socket = serverSocket.accept() //서버는 클라이언트가 접속할 때까지 여기서 대기 접속하면 다음으로 코드로 넘어감
                    //클라이언트와 데이터를 주고 받기 위한 통로구축
                    input = DataInputStream(socket.getInputStream()) //클라이언트로 부터 메세지를 받기 위한 통로
                    output = DataOutputStream(socket.getOutputStream()) //클라이언트로 메세지를 보내기 위한 통로
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Connected With Client Socket",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                //클라이언트가 접속을 끊을 때까지 무한반복하면서 클라이언트의 메세지 수신
                while (isConnected) {
                    try {
                        var msg = input?.readUTF()
                        msg = if (tv_chatting.text.toString().isEmpty()) {
                            "[RECV] $msg"
                        } else {
                            "${tv_chatting.text}\n [RECV] $msg"
                        }
                        //클라이언트로부터 읽어들인 메시지msg를 TextView에 출력한다. 안드로이드는 메인스레드가 아니면 UI변경 불가하므로 다음과같이 해줌.(토스트메세지도 마찬가지)
                        runOnUiThread {
                            tv_chatting.text = msg
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }.start() // Tread 실행
        }
    }


    @SuppressLint("SetTextI18n")
    fun sendMessage() {
        if (output == null) return

        var message = et_message.text.toString()
        Thread {
            try {
                runOnUiThread {
                    message = if (tv_chatting.text.toString().isEmpty()) {
                        "[SEND] $message"
                    } else {
                        "${tv_chatting.text}\n[SEND] $message"
                    }
                    tv_chatting.text = message
                }
                output?.writeUTF(et_message.text.toString())
                output?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }.start()
    }


    @Throws(UnknownHostException::class)
    private fun getLocalIpAddress(): String? {

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress

        return InetAddress.getByAddress(
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        )
            .hostAddress
    }

    override fun onStop() {
        super.onStop()
        try {
            socket.close()
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}