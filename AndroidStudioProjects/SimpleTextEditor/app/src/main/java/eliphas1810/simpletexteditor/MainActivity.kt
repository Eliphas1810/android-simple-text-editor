package eliphas1810.simpletexteditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.documentfile.provider.DocumentFile
import java.io.InputStreamReader
import java.nio.charset.Charset


private class Adapter(context: Context, lineList: List<String>, ): ArrayAdapter<String>(context, R.layout.view_line) {

    var maxLineNumberSize = 1

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var view: View? = view
        try {
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.view_line, viewGroup, false)
            }

            var line = getItem(position)

            var lineNumber = "" + (position + 1)

            view?.findViewById<TextView>(R.id.viewLine)?.text = lineNumber + "    " + line

        } catch (exception: Exception) {
            Toast.makeText(view?.context?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
        return view!!
    }
}


class MainActivity : AppCompatActivity() {


    companion object {
        const val FILE_NAME_KEY = "eliphas1810.simpletexteditor.FILE_NAME"
        const val CHARACTER_CODE_KEY = "eliphas1810.simpletexteditor.CHARACTER_CODE"
        const val LINE_BREAK_KEY = "eliphas1810.simpletexteditor.LINE_BREAK"
        const val TEXT_KEY = "eliphas1810.simpletexteditor.TEXT"
        const val CURSOR_TEXT_INDEX_KEY = "eliphas1810.simpletexteditor.CURSOR_TEXT_INDEX"
    }


    var fileNameTextView: TextView? = null

    var characterCodeEditText: EditText? = null

    var lineBreakEditText: EditText? = null

    var viewLineListView: ListView? = null


    private var text: String? = ""


    private var readActivityResultLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->

        if (activityResult.resultCode == RESULT_OK) {
            val intent = activityResult.data

            val uri: Uri? = intent?.data

            val documentFile = DocumentFile.fromSingleUri(applicationContext, uri!!)

            fileNameTextView?.text = documentFile?.name

            var characterCode = characterCodeEditText?.text?.toString()!!
            if (characterCode.isEmpty()) {
                characterCodeEditText?.setText(getString(R.string.character_code_name_default))
                characterCode = getString(R.string.character_code_name_default)
            }
            try {
                Charset.forName(characterCode)
            } catch (exception: Exception) {
                characterCodeEditText?.setText(getString(R.string.character_code_name_default))
                characterCode = getString(R.string.character_code_name_default)
            }

            InputStreamReader(contentResolver.openInputStream(uri!!), characterCode).use {
                text = it.readText()
            }

            if (text?.contains("\r\n") == true) {
                lineBreakEditText?.setText("\\r\\n")
            } else if (text?.contains("\r") == true) {
                lineBreakEditText?.setText("\\r")
            } else {
                lineBreakEditText?.setText("\\n")
            }

            text = text?.replace("\r\n", "\n")
            text = text?.replace("\r", "\n")
            var lineList: List<String> = text!!.split("\n")
            if (lineList.isEmpty()) {
               lineList = listOf("")
            }

            val adapter = viewLineListView?.adapter as Adapter
            adapter.clear()
            adapter.addAll(lineList)
            adapter.maxLineNumberSize = lineList.size
            adapter.notifyDataSetChanged()
        }
    }


    private var editActivityResultLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->

        if (activityResult.resultCode == RESULT_OK) {
            val intent = activityResult.data

            fileNameTextView?.text = intent?.getStringExtra(FILE_NAME_KEY)

            characterCodeEditText?.setText(intent?.getStringExtra(CHARACTER_CODE_KEY))

            lineBreakEditText?.setText(intent?.getStringExtra(LINE_BREAK_KEY))

            text = intent?.getStringExtra(TEXT_KEY)

            text = text?.replace("\r\n", "\n")
            text = text?.replace("\r", "\n")
            var lineList: List<String> = text!!.split("\n")
            if (lineList.isEmpty()) {
                lineList = listOf("")
            }

            val adapter = viewLineListView?.adapter as Adapter
            adapter.clear()
            adapter.addAll(lineList)
            adapter.maxLineNumberSize = lineList.size
            adapter.notifyDataSetChanged()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)


            fileNameTextView = findViewById(R.id.viewFileName)

            characterCodeEditText = findViewById(R.id.viewCharacterCodeName)

            lineBreakEditText = findViewById(R.id.viewLineBreak)


            viewLineListView = findViewById(R.id.viewLineList)

            val adapter = Adapter(this, listOf(""))
            adapter.maxLineNumberSize = 1
            viewLineListView?.adapter = adapter


            viewLineListView?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
                try {

                    text = text?.replace("\r\n", "\n")
                    text = text?.replace("\r", "\n")
                    var lineList: List<String> = text!!.split("\n")
                    if (lineList.isEmpty()) {
                        lineList = listOf("")
                    }

                    var cursorTextIndex = 0
                    if (1 <= position) {
                        cursorTextIndex = lineList.slice(0..(position - 1)).joinToString("\n").length + 1
                    }

                    val intent = Intent(this, Edit::class.java)
                    intent.putExtra(FILE_NAME_KEY, fileNameTextView?.text)
                    intent.putExtra(CHARACTER_CODE_KEY, characterCodeEditText?.text?.toString())
                    intent.putExtra(LINE_BREAK_KEY, lineBreakEditText?.text?.toString())
                    intent.putExtra(TEXT_KEY, text)
                    intent.putExtra(CURSOR_TEXT_INDEX_KEY, cursorTextIndex)
                    startActivity(intent)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.viewRead).setOnClickListener{ view ->
                try {

                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    readActivityResultLauncher?.launch(intent)

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.viewEdit).setOnClickListener{ view ->
                try {

                    val intent = Intent(this, Edit::class.java)
                    intent.putExtra(FILE_NAME_KEY, fileNameTextView?.text)
                    intent.putExtra(CHARACTER_CODE_KEY, characterCodeEditText?.text?.toString())
                    intent.putExtra(LINE_BREAK_KEY, lineBreakEditText?.text?.toString())
                    intent.putExtra(TEXT_KEY, text)
                    intent.putExtra(CURSOR_TEXT_INDEX_KEY, text?.length!!)
                    editActivityResultLauncher?.launch(intent)

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.viewCopyAll).setOnClickListener{ view ->
                try {

                    val clipboardManager = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    override fun onDestroy() {
        try {


            fileNameTextView = null

            characterCodeEditText = null

            lineBreakEditText = null


            viewLineListView = null


            text = null


            readActivityResultLauncher?.unregister()
            readActivityResultLauncher = null


            editActivityResultLauncher?.unregister()
            editActivityResultLauncher = null


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {

            super.onDestroy()
        }
    }
}
