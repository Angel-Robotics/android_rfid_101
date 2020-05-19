package com.example.rfidapplication

import android.Manifest
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException


const val PERMISSION_NFC = 1007

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null
    private var nfcManager: NfcManager? = null


    //    var textViewInfo: TextView? = null
    //    var textViewTagInfo : TextView ? =null
    public var textViewBlock: TextView? = null

    private var nfcFlags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_BARCODE or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V

    var mifareClassic: MifareClassic? = null
    var globalTag: Tag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //        textViewInfo = findViewById<TextView>(R.id.info)
//        textViewTagInfo = findViewById<TextView>(R.id.taginfo)
        textViewBlock = findViewById(R.id.tag_block)

        nfcManager = this.getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager?.defaultAdapter
        //        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                arrayOf(Manifest.permission.NFC),
                PERMISSION_NFC
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            nfcAdapter?.enableReaderMode(this, this, nfcFlags, null)
        }
        if (nfcAdapter == null) {
            Toast.makeText(
                this,
                "NFC NOT supported on this devices!",
                Toast.LENGTH_LONG
            ).show()
            finish()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(
                this,
                "NFC NOT Enabled!",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }

        btn_write.setOnClickListener {
            Log.e("MainActivity.kt", "Button Clicked")
            if (globalTag != null) {
                Log.e("Hello", globalTag!!.id.toString() + "")
                mifareClassic = MifareClassic.get(globalTag)
                mifareClassic!!.connect()
                var auth = false
                auth = mifareClassic!!.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT)
                if (auth) {
                    val text = "Hello, World!"
                    val value = text.toByteArray()
                    val toWrite = ByteArray(MifareClassic.BLOCK_SIZE)
                    for (i in 0 until MifareClassic.BLOCK_SIZE) {
                        if (i < value.size) toWrite[i] = value[i] else toWrite[i] = 0
                    }
                    mifareClassic!!.writeBlock(1, toWrite)
                    mifareClassic!!.close()
                }
            } else {
                Log.e("Hello", "Global Tag is null")
            }
        }

        btn_read.setOnClickListener {
            Log.e("Hello", "Read Button Clicked")
            if (globalTag != null) {
                Log.e("Hello", globalTag!!.id.toString() + "")
                mifareClassic = MifareClassic.get(globalTag)

                GlobalScope.launch {
                    mifareClassic!!.connect()
                    try {

//                        val toRead = mifareClassic!!.readBlock(1)
//                        Log.e("ReadTag", toRead.toString())
                        for (s in 0 until 16) {
                            Log.e("sector : ", "$s")
                            if (mifareClassic!!.authenticateSectorWithKeyA(
                                    s,
                                    MifareClassic.KEY_DEFAULT
                                )
                            ) {
                                for (b in 0 until 4) {
                                    val blockIndex = s * 4 + b
                                    Log.e(
                                        "test ",
                                        "block $blockIndex : " + mifareClassic!!.readBlock(
                                            blockIndex
                                        ).toHex() + " "
                                    )
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        mifareClassic!!.close()
                    }

                }


            } else {
                Log.e("Hello", "Global Tag is null")
            }
        }
    }

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
    fun ByteArray.toHex(): String {
        val result = StringBuffer()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS[firstIndex])
            result.append(HEX_CHARS[secondIndex])
            result.append(" ")
        }
        return result.toString()
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        val action = intent.action

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            Toast.makeText(
                this,
                "onResume() - ACTION_TAG_DISCOVERED",
                Toast.LENGTH_SHORT
            ).show()
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            globalTag = tag
            var tagInfo: String = tag.toString().toString() + "\n"
            tagInfo += "\nTag Id: \n"
            val tagId: ByteArray = tag.id
            tagInfo += "length = " + tagId.size + "\n"
            for (i in tagId.indices) {
                tagInfo += Integer.toHexString(tagId[i].toInt().and(0xFF)) + " "
            }
            tagInfo += "\n"
            val techList: Array<String> = tag.techList
            tagInfo += "\nTech List\n"
            tagInfo += "length = " + techList.size + "\n"
            for (i in techList.indices) {
                tagInfo += techList[i] + "\n "
            }
            info!!.text = tagInfo
            readMifareClassic(tag)
        } else {
            Toast.makeText(
                this,
                "onResume() : $action",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun readMifareClassic(tag: Tag): Boolean {
        var mifareClassicTag: MifareClassic = MifareClassic.get(tag)
        var typeInfoString = "--- MifareClassic tag ---\n"
        val type: Int = mifareClassicTag.type

        typeInfoString += when (type) {
            MifareClassic.TYPE_PLUS -> "MifareClassic.TYPE_PLUS\n"
            MifareClassic.TYPE_PRO -> "MifareClassic.TYPE_PRO\n"
            MifareClassic.TYPE_CLASSIC -> "MifareClassic.TYPE_CLASSIC\n"
            MifareClassic.TYPE_UNKNOWN -> "MifareClassic.TYPE_UNKNOWN\n"
            else -> "unknown...!\n"
        }
        val size: Int = mifareClassicTag.size
        typeInfoString += when (size) {
            MifareClassic.SIZE_1K -> "MifareClassic.SIZE_1K\n"
            MifareClassic.SIZE_2K -> "MifareClassic.SIZE_2K\n"
            MifareClassic.SIZE_4K -> "MifareClassic.SIZE_4K\n"
            MifareClassic.SIZE_MINI -> "MifareClassic.SIZE_MINI\n"
            else -> "unknown size...!\n"
        }

        val blockCount: Int = mifareClassicTag.blockCount
        typeInfoString += "BlockCount \t= $blockCount\n"
        val sectorCount: Int = mifareClassicTag.sectorCount
        typeInfoString += "SectorCount \t= $sectorCount\n"

        taginfo.text = typeInfoString
        ReadMifareClassicTask(mifareClassicTag, tag_block).execute()
        return true

    }

    /*
           MIFARE Classic tags are divided into sectors, and each sector is sub-divided into blocks.
           Block size is always 16 bytes (BLOCK_SIZE). Sector size varies.
           MIFARE Classic 1k are 1024 bytes (SIZE_1K), with 16 sectors each of 4 blocks.
           */
    private class ReadMifareClassicTask internal constructor(
        var taskTag: MifareClassic?,
        textViewBlock: TextView
    ) :
        AsyncTask<Void?, Void?, Void?>() {
        val textViewBlock: TextView? = textViewBlock
        var numOfBlock = 0
        val FIX_SECTOR_COUNT = 16
        var success = false
        val numOfSector = 16
        val numOfBlockInSector = 4
        var buffer =
            Array(
                numOfSector
            ) {
                Array(
                    numOfBlockInSector
                ) { ByteArray(MifareClassic.BLOCK_SIZE) }
            }

        override fun onPreExecute() {
            textViewBlock?.text = "Reading Tag, don't remove it!"
        }


        override fun onPostExecute(aVoid: Void?) { //display block
            if (success) {
                var stringBlock = ""
                for (i in 0 until numOfSector) {
                    stringBlock += "$i :\n"
                    for (j in 0 until numOfBlockInSector) {
                        for (k in 0 until MifareClassic.BLOCK_SIZE) {
                            stringBlock += String.format(
                                "%02X",
                                buffer[i][j][k].toInt().and(0xff)
                            ) + " "
                        }
                        stringBlock += "\n"
                    }
                    stringBlock += "\n"
                }
                textViewBlock?.text = stringBlock
            } else {
                textViewBlock?.text = "Fail to read Blocks!!!"
            }
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                taskTag!!.connect()
                for (s in 0 until numOfSector) {
                    if (taskTag!!.authenticateSectorWithKeyA(s, MifareClassic.KEY_DEFAULT)) {
                        for (b in 0 until numOfBlockInSector) {
                            val blockIndex = s * numOfBlockInSector + b
                            buffer[s][b] = taskTag!!.readBlock(blockIndex)
                        }
                    }
                }
                success = true
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (taskTag != null) {
                    try {
                        taskTag!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }
    }

    class writeMifareBlock(
        var taskTag: MifareClassic?
    ) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        Log.e("MainActivity", "tag Discovered")
        if (tag != null) {

            globalTag = tag

            runOnUiThread {
                var tagInfo: String = tag.toString().toString() + "\n"
                tagInfo += "\nTag Id: \n"
                val tagId: ByteArray = tag.id
                tagInfo += "length = " + tagId.size + "\n"
                for (i in tagId.indices) {
                    tagInfo += Integer.toHexString(tagId[i].toInt().and(0xFF)) + " "
                }
                tagInfo += "\n"
                val techList: Array<String> = tag.techList
                tagInfo += "\nTech List\n"
                tagInfo += "length = " + techList.size + "\n"
                for (i in techList.indices) {
                    tagInfo += techList[i] + "\n "
                }
                info!!.text = tagInfo
                readMifareClassic(tag)
            }
        }
    }
}









