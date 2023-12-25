package xyz.chz.bfm.ui.converter.config

import android.net.Uri
import org.json.JSONObject
import java.net.URLDecoder
import kotlin.random.Random

class ClashData(private val masuk: String = "", private val indent: Boolean = false) {

    fun proxyGroupBuilder(nameProxy: String, listProxy: String): String {
        val sb = StringBuilder()
        sb.appendLine("- name: $nameProxy")
        sb.appendLine("  type: select")
        sb.appendLine("  proxies:")
        sb.appendLine("    - urltest")
        sb.appendLine("    - loadbalance")
        sb.appendLine("    - fallback")
        sb.append(listProxy)

        sb.appendLine("- name: urltest")
        sb.appendLine("  type: url-test")
        sb.appendLine("  url: 'http://www.gstatic.com/generate_204'")
        sb.appendLine("  interval: 300")
        sb.appendLine("  proxies:")
        sb.append(listProxy)

        sb.appendLine("- name: loadbalance")
        sb.appendLine("  type: load-balance")
        sb.appendLine("  url: 'http://www.gstatic.com/generate_204'")
        sb.appendLine("  interval: 300")
        sb.appendLine("  # strategy: consistent-hashing # 可选 round-robin 和 sticky-sessions")
        sb.appendLine("  proxies:")
        sb.append(listProxy)

        sb.appendLine("- name: fallback")
        sb.appendLine("  type: fallback")
        sb.appendLine("  url: 'http://www.gstatic.com/generate_204'")
        sb.appendLine("  interval: 300")
        sb.appendLine("  proxies:")
        sb.append(listProxy)

        return sb.toString()
    }

    fun newVmessConfig(): String {
        val jo = JSONObject(masuk)
        val sb = StringBuilder()
        val idnt: String = if (indent) "  " else ""
        sb.appendLine(
            "${idnt}- name: ${
                jo.optString(
                    "ps",
                    jo.optString("add", "new")
                )
            }_vmess_${Random.nextInt(0, 7000)}"
        )
        sb.appendLine("    server: ${jo.optString("add", jo.optString("host", ""))}")
        sb.appendLine("    port: ${jo.getString("port")}")
        sb.appendLine("    type: vmess")
        sb.appendLine("    uuid: ${jo.getString("id")}")
        sb.appendLine("    alterId: ${jo.optString("aid", "0")}")
        sb.appendLine("    cipher: ${jo.optString("scy", "auto")}")
        sb.appendLine("    tls: ${jo.optString("tls", "") == "tls"}")
        sb.appendLine("    servername: ${jo.optString("sni", jo.optString("add", ""))}")
        sb.appendLine("    skip-cert-verify: true")
        sb.appendLine("    udp: true")
        when (jo.optString("net", "tcp")) {
            "ws" -> {
                sb.appendLine("    network: ws")
                sb.appendLine("    ws-opts:")
                sb.appendLine("      path: ${jo.optString("path", "/")}")
                sb.appendLine("      headers:")
                sb.append("        Host: ${jo.getString("host")}")
            }

            "grpc" -> {
                sb.appendLine("${idnt}  network: grpc")
                sb.appendLine("${idnt}  grpc-opts:")
                sb.append("${idnt}    grpc-service-name: ${jo.getString("path")}")
            }

            "h2" -> {
                sb.appendLine("${idnt}  network: h2")
                sb.appendLine("${idnt}  h2-opts:")
                sb.append("${idnt}    path: ${jo.optString("path", "/")}")
            }

            "tcp" -> {
                if (jo.optString("type", "") == "http") {
                    sb.appendLine("${idnt}  network: http")
                    sb.appendLine("${idnt}  http-opts:")
                    sb.append("${idnt}    path: ${jo.optString("path", "/")}")
                }
            }

            else -> throw Exception("${jo.getString("net")} not supported")
        }
        return sb.toString()
    }

    fun newVlessConfig(): String {
        val sb = StringBuilder()
        val idnt: String = if (indent) "  " else ""
        var url = masuk
        if (!url.contains("@")) url = ConfigUtil.safeDecodeURLB64(url)
        val uri = Uri.parse(url)
        sb.appendLine(
            "${idnt}- name: ${uri.fragment ?: "new"}_${uri.scheme}_${
                Random.nextInt(
                    0,
                    7000
                )
            }"
        )
        sb.appendLine("${idnt}  server: ${uri.host ?: ""}")
        sb.appendLine("${idnt}  port: ${uri.port}")
        sb.appendLine("${idnt}  type: vless")
        if (uri.userInfo == null || uri.userInfo!!.isEmpty()) throw Exception("no user info")
        sb.appendLine("${idnt}  uuid: ${uri.userInfo}")
        sb.appendLine(
            "${idnt}  tls: ${
                (ConfigUtil.getQueryParams(
                    uri,
                    "security"
                ) ?: "") == "tls"
            }"
        )
        sb.appendLine(
            "${idnt}  servername: ${
                ConfigUtil.getQueryParams(
                    uri,
                    "sni"
                ) ?: ConfigUtil.getQueryParams(uri, "host") ?: uri.host!!
            }"
        )
        sb.appendLine("${idnt}  skip-cert-verify: true")
        sb.appendLine("${idnt}  udp: true")
        if (ConfigUtil.getQueryParams(
                uri,
                "flow"
            ) != null
        ) sb.appendLine("${idnt}  flow: ${ConfigUtil.getQueryParams(uri, "flow")!!}")
        val type = ConfigUtil.getQueryParams(uri, "type") ?: "tcp"
        val decodePath =
            URLDecoder.decode(ConfigUtil.getQueryParams(uri, "path") ?: "", "UTF-8")
        val decodeHost =
            URLDecoder.decode(ConfigUtil.getQueryParams(uri, "host") ?: "", "UTF-8")
        when (type) {
            "ws" -> {
                sb.appendLine("${idnt}  network: ws")
                sb.appendLine("${idnt}  ws-opts:")

                sb.appendLine("${idnt}    path: $decodePath")
                sb.appendLine("${idnt}    headers:")
                sb.append("${idnt}      Host: $decodeHost")
            }

            "tcp" -> {}
            "http" -> {}
            "grpc" -> {
                sb.appendLine("${idnt}  network: grpc")
                sb.appendLine("${idnt}  grpc-opts:")
                sb.append(
                    "${idnt}    grpc-service-name: ${
                        ConfigUtil.getQueryParams(
                            uri,
                            "serviceName"
                        ) ?: ""
                    }"
                )
            }

            else -> throw Exception("$type not supported")
        }
        return sb.toString()
    }

    fun newTrojanConfig(): String {
        val sb = StringBuilder()
        val idnt: String = if (indent) "  " else ""
        var url = masuk
        if (!url.contains("@")) url = ConfigUtil.safeDecodeURLB64(url)
        val uri = Uri.parse(url)
        sb.appendLine(
            "${idnt}- name: ${uri.fragment ?: "new"}_${uri.scheme}_${
                Random.nextInt(
                    0,
                    7000
                )
            }"
        )
        sb.appendLine("${idnt}  server: ${uri.host ?: ""}")
        sb.appendLine("${idnt}  port: ${uri.port}")
        sb.appendLine("${idnt}  type: trojan")
        if (uri.userInfo == null || uri.userInfo!!.isEmpty()) throw Exception("no user info")
        sb.appendLine("${idnt}  password: ${uri.userInfo}")
        sb.appendLine(
            "${idnt}  sni: ${
                ConfigUtil.getQueryParams(
                    uri,
                    "sni"
                ) ?: ConfigUtil.getQueryParams(uri, "host") ?: uri.host!!
            }"
        )
        sb.appendLine("${idnt}  skip-cert-verify: true")
        sb.appendLine("${idnt}  udp: true")
        if (ConfigUtil.getQueryParams(
                uri,
                "flow"
            ) != null
        ) sb.appendLine("${idnt}  flow: ${ConfigUtil.getQueryParams(uri, "flow")!!}")
        val alpnStr = URLDecoder.decode(ConfigUtil.getQueryParams(uri, "alpn") ?: "", "UTF-8")
        if (alpnStr != "") sb.appendLine("${idnt}  alpn: ${alpnStr.split(",")}")

        val type = ConfigUtil.getQueryParams(uri, "type") ?: "tcp"
        val decodePath =
            URLDecoder.decode(ConfigUtil.getQueryParams(uri, "path") ?: "", "UTF-8")
        val decodeHost =
            URLDecoder.decode(ConfigUtil.getQueryParams(uri, "host") ?: "", "UTF-8")

        when (type) {
            "ws" -> {
                sb.appendLine("${idnt}  network: ws")
                sb.appendLine("${idnt}  ws-opts:")

                sb.appendLine("${idnt}    path: $decodePath")
                sb.appendLine("${idnt}    headers:")
                sb.append("${idnt}      Host: $decodeHost")
            }

            "tcp" -> {}
            "http" -> {}
            "grpc" -> {
                sb.appendLine("${idnt}  network: grpc")
                sb.appendLine("${idnt}  grpc-opts:")
                sb.append(
                    "${idnt}    grpc-service-name: ${
                        ConfigUtil.getQueryParams(
                            uri,
                            "serviceName"
                        ) ?: ""
                    }"
                )
            }

            else -> throw Exception("$type not supported")
        }

        return sb.toString()
    }

}
