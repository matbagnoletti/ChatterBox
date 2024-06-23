package edu.avolta.tpsit.chatterbox

import com.google.gson.Gson
import edu.avolta.tpsit.multicastudpsocketchat.gestione.OutputType
import edu.avolta.tpsit.multicastudpsocketchat.gestione.ProjectOutput
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


class WSHandler {
    private val client = OkHttpClient().newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)  // connessione
        .readTimeout(10, TimeUnit.SECONDS)     // lettura
        .writeTimeout(10, TimeUnit.SECONDS)   // scrittura
        .build()
    private val mainUrl = "https://chatterbox.altervista.org/api/v2/"
    private val gson = Gson()
    
    /**
     * Ricerca un gruppo di chat
     * @param nome il nome del gruppo
     * @param psw la password del gruppo
     * @return un RRWebService con i dati del gruppo trovato in caso di successo, APIError in caso di errore
     */
    @Synchronized
    fun cercaGruppo(nome: String, psw: String, callback: (Any) -> Unit) {
        val url = "$mainUrl?nomeChat=${nome}&pswChat=${psw}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    callback(APIError("Impossibile raggiungere il server nel tempo previsto (timeout)"))
                } else {
                    callback(APIError("Impossibile raggiungere il server (errore di connessione)"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        when (response.code) {
                            200 -> {
                                val risposta = response.body.string()
                                val gruppoTrovato = gson.fromJson(risposta, RRWebService::class.java)
                                if (gruppoTrovato != null) {
                                    callback(gruppoTrovato)
                                } else {
                                    callback(APIError("Gruppo trovato ma non è stato possibile recuperare i dati"))
                                }
                            }
                            
                            401 -> callback(APIError("Errore: la password inserita è errata"))
                            404 -> callback(APIError("Errore: il gruppo ricercato non esiste"))
                            else -> callback(APIError("Ops! Si è verificato un errore"))
                        }
                    } else {
                        callback(
                            when (response.code) {
                                401 -> APIError("Errore: la password inserita è errata")
                                404 -> APIError("Errore: il gruppo ricercato non esiste")
                                else -> APIError("Ops! Si è verificato un errore")
                            }
                        )
                    }
                } catch (e: Exception) {
                    callback(APIError("Ops! Si è verificato un errore"))
                } finally {
                    response.close()
                }
            }
        })
    }

    /**
     * Crea un nuovo gruppo di chat
     * @param nome il nome del gruppo
     * @param psw la password del gruppo
     * @return un RRWebService con i dati del gruppo creato in caso di successo, APIError in caso di errore
     */
    @Synchronized
    fun creaGruppo(nome: String, psw: String, callback: (Any) -> Unit) {
        val group = mapOf("nomeChat" to nome, "pswChat" to psw, "vChat" to "2.1.1")
        val json = gson.toJson(group)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(mainUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    callback(APIError("Impossibile raggiungere il server nel tempo previsto (timeout)"))
                } else {
                    callback(APIError("Impossibile raggiungere il server (errore di connessione)"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        when (response.code) {
                            201 -> {
                                val risposta = response.body.string()
                                val gruppoCreato = gson.fromJson(risposta, RRWebService::class.java)
                                if (gruppoCreato == null) {
                                    callback(APIError("Il gruppo creato correttamente ma non è stato possibile recuperare i dati"))
                                } else {
                                    callback(gruppoCreato)
                                }
                            }

                            401 -> APIError("Errore: la password inserita è errata")
                            409 -> APIError("Errore: il gruppo esiste già")
                            else -> callback(APIError("Ops! Si è verificato un errore"))
                        }
                    } else {
                        callback(
                            when (response.code) {
                                401 -> APIError("Errore: la password inserita è errata")
                                409 -> APIError("Errore: il gruppo esiste già")
                                else -> APIError("Ops! Si è verificato un errore")
                            }
                        )
                    }
                } catch (e: Exception) {
                    callback(APIError("Ops! Si è verificato un errore"))
                } finally {
                    response.close()
                }
            }
        })
    }

    /**
     * Elimina un gruppo di chat
     * @param id l'identificativo del gruppo
     * @param psw la password del gruppo
     * @return un messaggio di successo o errore
     */
    @Synchronized
    fun eliminaGruppo(id: Int, psw: String, callback: (Any) -> Unit) {
        val data = mapOf("idChat" to id, "pswChat" to psw)
        val json = gson.toJson(data)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(mainUrl)
            .delete(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    callback(APIError("Impossibile raggiungere il server nel tempo previsto (timeout)"))
                } else {
                    callback(APIError("Impossibile raggiungere il server (errore di connessione)"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val risposta = response.body.string()
                    callback(risposta)
                } else {
                    callback(
                        when(response.code) {
                            401 -> APIError("Errore: la password inserita è errata")
                            404 -> APIError("Errore: il gruppo ricercato non esiste")
                            else -> APIError("Ops! Si è verificato un errore")
                        }
                    )
                }
            }
        })
    }

    /**
     * Disconnette il client e libera le risorse
     */
    fun disconnetti() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        if (client.cache != null) {
            try {
                client.cache!!.close()
            } catch (e: IOException) {
                ProjectOutput.stampa(e.message, OutputType.STDERR)
            }
        }
    }
}