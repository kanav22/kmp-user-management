package com.sliide.usermanager.config

actual val goRestToken: String = System.getProperty("GOREST_TOKEN", "")
