package com.example.secureflow.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SimpleVpnService : VpnService(), CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "SecureFlowVPN"
        private const val CHANNEL_ID = "secureflow_vpn"

        // EMULATOR → host machine:
        private const val PROXY_HOST = "172.104.236.190"
        private const val PROXY_PORT = 22

        private const val VPN_IP = "10.0.0.2"
        private const val VPN_PREFIX = 24              // /24
        private const val VPN_IP_CIDR = "10.0.0.2/24"  // pass as CIDR to tun2socks
      //  private const val DNS1 = "1.1.1.1"
      //  private const val DNS2 = "8.8.8.8"
        private const val DNS1 = "208.67.222.222"  // OpenDNS primary
        private const val DNS2 = "208.67.220.220"  // OpenDNS secondary
        private const val MTU = 1500
    }

    private var tun: ParcelFileDescriptor? = null
    private var tun2socks: Process? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        launch(Dispatchers.IO) {
            try {
                startTunnel()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VPN", e)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SecureFlow VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("SecureFlow VPN")
            .setContentText("Running on emulator (x86_64)")
            .setOngoing(true)
            .build()
        startForeground(1, notif)
    }

    //
    //private fun startTunnel() {
      //  if (tun != null) return

       // val builder = Builder()
         //   .setSession("SecureFlow")
         //   .addAddress(VPN_IP, VPN_PREFIX)  // /24 to match tun2socks
          //  .addDnsServer(DNS1)
          //  .addDnsServer(DNS2)
           // .addRoute("0.0.0.0", 0)
          //  .setMtu(MTU)

        // exclude our own app → child process reaches proxy directly
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //    try {
           //     builder.addDisallowedApplication(packageName)
        //    } catch (e: Exception) {
           //     Log.w(TAG, "addDisallowedApplication failed: ${e.message}")
         //   }
       // } else {
       //     Log.w(TAG, "addDisallowedApplication requires API 24+ and is skipped.")
      //  }

      //  tun = builder.establish() ?: throw IllegalStateException("Failed to establish TUN")
      //  Log.i(TAG, "TUN established fd=${tun!!.fd}")

      //  startTun2Socks(tun!!)
   // }

//    private fun startTun2Socks(tunFd: ParcelFileDescriptor) {
//       //  On emulator, prefer the adb-pushed binary path
//      //  val execPath = "/data/local/tmp/tun2socks"  // <-- emulator-only
//        val execPath = extractTun2SocksBinary()
//        val deviceArg = "tun://fd=${tunFd.fd},ipv4=$VPN_IP/$VPN_PREFIX,mtu=$MTU,dns=$DNS1,$DNS2"
//        val proxyArg  = "socks5://$PROXY_HOST:$PROXY_PORT"
//
//        val cmd = listOf(
//            execPath,
//            "-device", deviceArg,
//            "-proxy", proxyArg,
//            "-tcp-auto-tuning",
//            "-loglevel", "info"
//       )
//        Log.i(TAG, "Launching: ${cmd.joinToString(" ")}")
//        tun2socks = ProcessBuilder(cmd).redirectErrorStream(true).start()
//
//        launch(Dispatchers.IO) {
//            tun2socks?.inputStream?.bufferedReader()?.forEachLine { Log.d("tun2socks", it) }
//        }
//    }


    private fun startTun2Socks(tunFd: ParcelFileDescriptor) {
        val execPath = extractTun2SocksBinary()

        // Set execute permissions before starting
        setExecutePermissions(execPath)

        // Verify permissions
        val file = File(execPath)
        if (!file.canExecute()) {
            Log.e(TAG, "Binary still not executable after setting permissions")
            return
        }

        val deviceArg = "tun://fd=${tunFd.fd},ipv4=$VPN_IP/$VPN_PREFIX,mtu=$MTU,dns=$DNS1,$DNS2"
        val proxyArg = "socks5://$PROXY_HOST:$PROXY_PORT"

        val cmd = listOf(
            execPath,
            "-device", deviceArg,
            "-proxy", proxyArg,
            "-tcp-auto-tuning",
            "-loglevel", "info"
        )

        Log.i(TAG, "Launching: ${cmd.joinToString(" ")}")

        try {
            tun2socks = ProcessBuilder(cmd).redirectErrorStream(true).start()
            launch(Dispatchers.IO) {
                tun2socks?.inputStream?.bufferedReader()?.forEachLine {
                    Log.d("tun2socks", it)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start tun2socks: ${e.message}")
        }
    }
      // private fun startTunnel() {
       //    if (tun != null) return

        //   val builder = Builder()
         //      .setSession("SecureFlow")
         //      .addAddress(VPN_IP, VPN_PREFIX)  // /24 to match tun2socks
         //      .addDnsServer(DNS1)
         //      .addDnsServer(DNS2)
         //      .addRoute("0.0.0.0", 0)
         //      .setMtu(MTU)

           // exclude our own app → child process reaches proxy directly
        //   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //       try {
         //          builder.addDisallowedApplication(packageName)
         //      } catch (e: Exception) {
         //          Log.w(TAG, "addDisallowedApplication failed: ${e.message}")
         //      }
        //   } else {
        //       Log.w(TAG, "addDisallowedApplication requires API 24+ and is skipped.")
       //    }

        //   tun = builder.establish() ?: throw IllegalStateException("Failed to establish TUN")
       //    Log.i(TAG, "TUN established fd=${tun!!.fd}")

       //    startTun2Socks(tun!!)
     //  }
//       fun testInternetConnectivity(): Boolean {
//           return try {
//               val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8")
//               val exitCode = process.waitFor()
//               exitCode == 0
//           } catch (e: Exception) {
//               false
//           }
//       }
//    fun testDnsConnectivity(): Boolean {
//        return try {
//            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 google.com")
//            val exitCode = process.waitFor()
//            exitCode == 0
//        } catch (e: Exception) {
//            false
//        }
//    }
       private fun startTunnel() {
           if (tun != null) return

           val builder = Builder()
               .setSession("SecureFlow")
               .addAddress(VPN_IP, VPN_PREFIX)
               .addDnsServer(DNS1)
               .addDnsServer(DNS2)
               .addRoute("0.0.0.0", 0)
               .setMtu(MTU)

           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
               try {
                   builder.addDisallowedApplication(packageName)
               } catch (e: Exception) {
                   Log.w(TAG, "addDisallowedApplication failed: ${e.message}")
               }
           } else {
               Log.w(TAG, "addDisallowedApplication requires API 24+ and is skipped.")
           }

           tun = builder.establish() ?: throw IllegalStateException("Failed to establish TUN")
           Log.i(TAG, "TUN established fd=${tun!!.fd}")

           // Test connectivity after VPN established
           Log.i(TAG, "Internet connectivity test passed: ${testInternetConnectivity()}")

           startTun2Socks(tun!!)
       }



   // private fun extractTun2SocksBinary(): String {
        // Prefer emulator ABIs first
     //   val preferred = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
     //   Log.i(TAG, "SUPPORTED_ABIS=${Build.SUPPORTED_ABIS.joinToString()}")

        // decimal modes: 0700=448, 0755=493
      //  val MODE_0700 = 448
      //  val MODE_0755 = 493

        // Ensure an exec-friendly dir: <codeCacheDir>/bin
     //   val binDir = File(codeCacheDir, "bin").apply {
      //      if (!exists()) mkdirs()
        //    try { Os.chmod(absolutePath, MODE_0755) } catch (_: ErrnoException) {}
      //  }

      //  for (abi in preferred) {
       //     val assetPath = "tun2socks/$abi/tun2socks"
        //    val out = File(binDir, "tun2socks_$abi")
        //    try {
                // Copy (always overwrite to avoid stale perms)
          //      assets.open(assetPath).use { ins ->
           //         FileOutputStream(out).use { outs -> ins.copyTo(outs) }
           //     }
                // Tight owner-only perms (some SELinux policies prefer this)
              //  try { Os.chmod(out.absolutePath, MODE_0700) } catch (_: ErrnoException) {}
            //    out.setReadable(true, false)
                // out.setExecutable(true, false)

               // Log.i(TAG, "Using tun2socks ABI=$abi at ${out.absolutePath}, canExec=${out.canExecute()}")
               // if (out.canExecute()) return out.absolutePath
           // } catch (e: Exception) {
             //   Log.w(TAG, "Asset missing for $abi ($assetPath): ${e.message}")
           // }
       // }
       // throw IllegalStateException("No runnable tun2socks asset. Ensure assets/tun2socks/x86_64/tun2socks exists.")
    // }
//               private fun extractTun2SocksBinary(): String {
//                   val preferred = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
//                   val MODE_0700 = 448
//                   val MODE_0755 = 493
//
//                   val binDir = File(codeCacheDir, "bin").apply {
//                       if (!exists()) mkdirs()
//                       try { Os.chmod(absolutePath, MODE_0755) } catch (_: ErrnoException) {}
//                   }
//
//                   for (abi in preferred) {
//                       val assetPath = "tun2socks/$abi/tun2socks"
//                       val out = File(binDir, "tun2socks_$abi")
//                       try {
//                           // Copy binary file
//                           assets.open(assetPath).use { ins ->
//                               FileOutputStream(out).use { outs ->
//                                   ins.copyTo(outs)
//                               }
//                           }
//
//                           // Set permissions
//                           try { Os.chmod(out.absolutePath, MODE_0700) } catch (_: ErrnoException) {}
//                           out.setReadable(true, false)
//                           out.setExecutable(true, false)
//
//                           // Verify copy success
//                           if (out.exists() && out.canExecute()) {
//                               Log.i(TAG, "Binary copied successfully to ${out.absolutePath} with executable permission")
//                               return out.absolutePath
//                           } else {
//                               Log.e(TAG, "Binary copy failed or not executable: ${out.absolutePath}")
//                           }
//                       } catch (e: Exception) {
//                           Log.e(TAG, "Exception copying binary for ABI $abi: ${e.message}")
//                       }
//                   }
//
//                   throw IllegalStateException("No runnable tun2socks asset found.")
//               }

    private fun extractTun2SocksBinary(): String {
        val preferred = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
        val MODE_0700 = 448
        val MODE_0755 = 493

        val binDir = File(codeCacheDir, "bin").apply {
            if (!exists()) mkdirs()
            try {
                Os.chmod(absolutePath, MODE_0755)
            } catch (e: ErrnoException) {
                Log.w(TAG, "Failed to chmod bin directory: ${e.message}")
            }
        }

        for (abi in preferred) {
            val assetPath = "tun2socks/$abi/tun2socks"
            val out = File(binDir, "tun2socks_$abi")
            try {
                // Copy binary file
                assets.open(assetPath).use { ins ->
                    FileOutputStream(out).use { outs ->
                        ins.copyTo(outs)
                    }
                }

                // Set permissions using multiple methods for reliability
                try {
                    Os.chmod(out.absolutePath, MODE_0700)
                } catch (e: ErrnoException) {
                    Log.w(TAG, "Os.chmod failed: ${e.message}")
                }

                out.setReadable(true, false)
                out.setExecutable(true, false)

                // Try chmod as fallback
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "755", out.absolutePath))
                } catch (e: Exception) {
                    Log.w(TAG, "chmod command failed: ${e.message}")
                }

                // Verify
                if (out.exists() && out.canExecute()) {
                    Log.i(TAG, "Binary ready: ${out.absolutePath}, size: ${out.length()} bytes")
                    return out.absolutePath
                } else {
                    Log.e(TAG, "Binary not executable: ${out.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Asset missing for $abi ($assetPath): ${e.message}")
            }
        }
        throw IllegalStateException("No runnable tun2socks asset found.")
    }

    fun testInternetConnectivity(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun testDnsConnectivity(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 google.com")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tun2socks?.destroy() } catch (_: Exception) {}
        tun2socks = null
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        cancel()
    }
}
//    override fun onDestroy() {
//        super.onDestroy()
//        try { tun2socks?.destroy() } catch (_: Exception) {}
//        tun2socks = null
//        try { tun?.close() } catch (_: Exception) {}
//        tun = null
//        cancel()
//    }
//}



//dema
private fun setExecutePermissions(filePath: String) {
    try {
        val file = File(filePath)
        file.setExecutable(true)
        file.setReadable(true)
    } catch (e: Exception) {
        Log.e("VPNService", "Failed to set execute permissions: ${e.message}")
    }
}

