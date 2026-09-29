package com.example.oxumetro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // =====================================================
    // IP DO ESP32
    // =====================================================

    private val esp32IP = "192.168.4.1"


    // =====================================================
    // COMPONENTES DA INTERFACE
    // =====================================================

    private lateinit var txtStatus: TextView
    private lateinit var txtLed: TextView
    private lateinit var txtFrequency: TextView

    private lateinit var editFrequency: EditText
    private lateinit var btnApplyFrequency: Button


    // =====================================================
    // HANDLER
    // =====================================================

    private val handler =
        Handler(Looper.getMainLooper())


    // Impede várias requisições simultâneas
    private var requestRunning = false


    // =====================================================
    // ATUALIZAÇÃO AUTOMÁTICA
    // =====================================================

    private val updateRunnable = object : Runnable {

        override fun run() {

            if (!requestRunning) {
                getData()
            }

            // Consulta novamente depois de 500 ms
            handler.postDelayed(
                this,
                100
            )
        }
    }


    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )


        // =================================================
        // LOCALIZA COMPONENTES
        // =================================================

        txtStatus =
            findViewById(R.id.txtStatus)

        txtLed =
            findViewById(R.id.txtLed)

        txtFrequency =
            findViewById(R.id.txtFrequency)

        editFrequency =
            findViewById(R.id.editFrequency)

        btnApplyFrequency =
            findViewById(R.id.btnApplyFrequency)


        // =================================================
        // BOTÃO APLICAR FREQUÊNCIA
        // =================================================

        btnApplyFrequency.setOnClickListener {

            val frequency =
                editFrequency
                    .text
                    .toString()
                    .toIntOrNull()


            // Verifica se a frequência é válida

            if (
                frequency != null &&
                frequency in 1..3000
            ) {

                setFrequency(frequency)

            } else {

                txtStatus.text =
                    "Digite uma frequência entre 1 e 3000 Hz"
            }
        }


        // =================================================
        // INICIA ATUALIZAÇÃO
        // =================================================

        handler.post(
            updateRunnable
        )
    }


    // =====================================================
    // BUSCA DADOS DO ESP32
    // =====================================================

    private fun getData() {

        requestRunning = true

        thread {

            try {

                val response =
                    httpGet(
                        "http://$esp32IP/data"
                    )


                // Converte resposta para JSON

                val json =
                    JSONObject(response)


                val frequency =
                    json.getInt("frequency")

                val led =
                    json.getString("led")


                // Atualiza interface

                runOnUiThread {

                    txtStatus.text =
                        "ESP32 conectado"

                    txtLed.text =
                        "LED atual: $led"

                    txtFrequency.text =
                        "Frequência: $frequency Hz"

                }


            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text =
                        "ESP32 desconectado"

                    txtLed.text =
                        "LED atual: --"

                }

            } finally {

                requestRunning = false
            }
        }
    }


    // =====================================================
    // ALTERA FREQUÊNCIA
    // =====================================================

    private fun setFrequency(
        frequency: Int
    ) {

        // Evita conflito com consulta automática

        if (requestRunning) {
            return
        }

        requestRunning = true


        thread {

            try {

                val response =
                    httpGet(
                        "http://$esp32IP/setFrequency?value=$frequency"
                    )


                // Converte resposta para JSON

                val json =
                    JSONObject(response)


                val newFrequency =
                    json.getInt("frequency")


                // Atualiza interface

                runOnUiThread {

                    txtStatus.text =
                        "ESP32 conectado"

                    txtFrequency.text =
                        "Frequência: $newFrequency Hz"

                    editFrequency.setText(
                        newFrequency.toString()
                    )

                }


            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text =
                        "Erro ao alterar frequência"

                }

            } finally {

                requestRunning = false
            }
        }
    }


    // =====================================================
    // REQUISIÇÃO HTTP GET
    // =====================================================

    private fun httpGet(
        address: String
    ): String {

        val url =
            URL(address)


        val connection =
            url.openConnection()
                    as HttpURLConnection


        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            2000

        connection.readTimeout =
            2000


        connection.connect()


        return connection
            .inputStream
            .bufferedReader()
            .use {
                it.readText()
            }
    }


    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        handler.removeCallbacks(
            updateRunnable
        )

        super.onDestroy()
    }
}