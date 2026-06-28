package com.kanav.usermanager.config

actual val goRestToken: String = System.getProperty("GOREST_TOKEN", "")
