package eliphas1810.simpletexteditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.Charset


//テキスト編集履歴
data class History (
    var text: String = "", //テキスト編集履歴を保持した時点のテキスト
    var cursorIndex: Int = 0 //テキスト編集履歴を保持した時点のテキスト内のカーソルのインデックス番号
) {
}


class Edit : AppCompatActivity(), TextWatcher {


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

    var editText: EditText? = null


    var fileName: String? = null
    var editingFileName: String? = null //テキスト編集中を表すためのファイル名


    private var historyList: MutableList<History>? = mutableListOf() //テキスト編集履歴の一覧
    private var historyIndex: Int? = -1


    private var saveActivityResultLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->

        if (activityResult.resultCode == RESULT_OK) {
            val intent = activityResult.data

            val uri: Uri? = intent?.data

            val documentFile = DocumentFile.fromSingleUri(applicationContext, uri!!)

            fileName = documentFile?.name
            editingFileName = getString(R.string.editing_file_name_prefix) + fileName

            fileNameTextView?.text = fileName

            editText = findViewById<EditText>(R.id.editText)

            var text = editText?.text?.toString()

            var lineBreak = "\n"
            if (lineBreakEditText?.text?.toString() == "\\n") {
                lineBreak = "\n"
            } else if (lineBreakEditText?.text?.toString() == "\\r\\n") {
                lineBreak = "\r\n"
            } else if (lineBreakEditText?.text?.toString() == "\\r") {
                lineBreak = "\r"
            } else {
                lineBreak = "\n"
            }

            if (lineBreak == "\n") {
                lineBreakEditText?.setText("\\n")
            } else if (lineBreak == "\r\n") {
                lineBreakEditText?.setText("\\r\\n")
            } else if (lineBreak == "\r") {
                lineBreakEditText?.setText("\\r")
            }

            text = text?.replace("\r\n", "\n")
            text = text?.replace("\r", "\n")
            text = text?.replace("\n", lineBreak)

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

            //contentResolver.openOutputStream()で"wt"モードを指定しないと、書き込み前のテキストの文字数が大きい場合、書き込み前のテキストの先頭の一部を置換するような形に成ってしまいます。
            OutputStreamWriter(contentResolver.openOutputStream(uri!!, "wt"), characterCode).use {
                BufferedWriter(it).use {
                    it.write(text)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.edit)


            //戻るボタン、戻るジェスチャーを無効化
            onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })


            fileNameTextView = findViewById(R.id.editFileName)

            characterCodeEditText = findViewById(R.id.editCharacterCodeName)

            lineBreakEditText = findViewById(R.id.editLineBreak)

            editText = findViewById<EditText>(R.id.editText)

            fileName = getIntent()?.getStringExtra(FILE_NAME_KEY)
            editingFileName = getString(R.string.editing_file_name_prefix) + fileName

            fileNameTextView?.text = fileName

            characterCodeEditText?.setText(getIntent()?.getStringExtra(CHARACTER_CODE_KEY))

            lineBreakEditText?.setText(getIntent()?.getStringExtra(LINE_BREAK_KEY))

            editText?.setText(getIntent()?.getStringExtra(TEXT_KEY))
            editText?.setSelection(getIntent()?.getIntExtra(CURSOR_TEXT_INDEX_KEY, 0)!!)

            historyList?.clear()
            historyList?.add(History(editText?.text?.toString()!!, editText?.text?.toString()?.length!!))
            historyIndex = historyList?.size!! - 1

            findViewById<Button>(R.id.editUndo).isEnabled = false
            findViewById<Button>(R.id.editRedo).isEnabled = false


            findViewById<Button>(R.id.editView).setOnClickListener{ view ->
                try {

                    historyList?.clear()
                    historyIndex = -1

                    findViewById<Button>(R.id.editUndo).isEnabled = false
                    findViewById<Button>(R.id.editRedo).isEnabled = false

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra(FILE_NAME_KEY, fileName)
                    intent.putExtra(CHARACTER_CODE_KEY, characterCodeEditText?.text?.toString())
                    intent.putExtra(LINE_BREAK_KEY, lineBreakEditText?.text?.toString())
                    intent.putExtra(TEXT_KEY, editText?.text.toString())
                    setResult(RESULT_OK, intent)

                    fileName = null
                    editingFileName = null

                    finish()

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editSave).setOnClickListener{ view ->
                try {


                    if (historyIndex!! <= -1) {
                        return@setOnClickListener
                    }

                    while (historyIndex!! < historyList?.size!! - 1) {
                        historyList?.removeLast()
                    }

                    historyList?.add(History(editText?.text?.toString()!!, editText?.selectionEnd ?: editText?.text?.toString()?.length!!))
                    historyIndex = historyList?.size!! - 1

                    findViewById<Button>(R.id.editUndo).isEnabled = true
                    findViewById<Button>(R.id.editRedo).isEnabled = false

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    saveActivityResultLauncher?.launch(intent)

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editUndo).setOnClickListener{ view ->
                try {


                    if (historyIndex!! <= 0) {
                        return@setOnClickListener
                    }

                    historyIndex = historyIndex!! - 1

                    editText?.setText(historyList?.get(historyIndex!!)?.text!!)
                    editText?.setSelection(historyList?.get(historyIndex!!)?.cursorIndex!!)

                    findViewById<Button>(R.id.editRedo).isEnabled = true

                    if (historyIndex == 0) {

                        findViewById<Button>(R.id.editUndo).isEnabled = false

                        fileNameTextView?.text = fileName

                    } else {
                        fileNameTextView?.text = editingFileName
                    }


                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editRedo).setOnClickListener{ view ->
                try {


                    if (historyIndex!! <= -1) {
                        return@setOnClickListener
                    }

                    if (historyList?.size!! - 1 <= historyIndex!!) {
                        return@setOnClickListener
                    }

                    historyIndex = historyIndex!! + 1

                    editText?.setText(historyList?.get(historyIndex!!)?.text!!)
                    editText?.setSelection(historyList?.get(historyIndex!!)?.cursorIndex!!)

                    findViewById<Button>(R.id.editUndo).isEnabled = true

                    fileNameTextView?.text = editingFileName

                    if (historyList?.size!! - 1 <= historyIndex!!) {

                        findViewById<Button>(R.id.editRedo).isEnabled = false
                    }


                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editSearch).setOnClickListener{ view ->
                try {


                    val searchPattern = findViewById<EditText>(R.id.editSearchPattern)?.text?.toString()!!

                    if (searchPattern.isEmpty()) {
                        Toast.makeText(applicationContext, getString(R.string.empty_search_pattern), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    var searchRegex: Regex? = null
                    try {
                        searchRegex = Regex(searchPattern)
                    } catch (exception: Exception) {
                        Toast.makeText(applicationContext, getString(R.string.wrong_search_pattern) + exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }

                    if (editText?.hasFocus() == false) {
                        editText?.requestFocus()
                        editText?.setSelection(0)
                    }

                    val text = editText?.text?.toString()!!
                    val searchStartIndex = editText?.selectionEnd!!

                    val matchResult = searchRegex?.find(text, searchStartIndex)

                    if (matchResult == null) {
                        Toast.makeText(applicationContext, getString(R.string.search_result_zero), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    editText?.setSelection(matchResult?.range?.start!!, matchResult?.range?.endInclusive!! + 1)


                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editReplace).setOnClickListener{ view ->
                try {


                    val searchPattern = findViewById<EditText>(R.id.editSearchPattern)?.text?.toString()!!
                    val replaceText = findViewById<EditText>(R.id.editReplaceText)?.text?.toString()!!

                    if (searchPattern.isEmpty()) {
                        Toast.makeText(applicationContext, getString(R.string.empty_search_pattern), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    var searchRegex: Regex? = null
                    try {
                        searchRegex = Regex(searchPattern)
                    } catch (exception: Exception) {
                        Toast.makeText(applicationContext, getString(R.string.wrong_search_pattern) + exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }

                    if (editText?.hasFocus() == false) {
                        editText?.requestFocus()
                        editText?.setSelection(0)
                    }

                    var text = editText?.text?.toString()!!
                    val searchStartIndex = editText?.selectionStart!!

                    val matchResult = searchRegex?.find(text, searchStartIndex)

                    if (matchResult == null) {
                        Toast.makeText(applicationContext, getString(R.string.search_result_zero), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    val startIndex = matchResult?.range?.start!!
                    val endIndex = matchResult?.range?.endInclusive!!

                    val replacedText = text.substring(startIndex, endIndex + 1).replace(Regex(searchPattern), replaceText)

                    text = text.substring(0, startIndex) + replacedText + text.substring(endIndex + 1)

                    while (historyIndex!! < historyList?.size!! - 1) {
                        historyList?.removeLast()
                    }

                    historyList?.add(History(text, startIndex + replaceText.length))
                    historyIndex = historyList?.size!! - 1

                    editText?.setText(text)
                    editText?.setSelection(startIndex + replaceText.length)

                    fileNameTextView?.text = editingFileName

                    findViewById<Button>(R.id.editUndo).isEnabled = true
                    findViewById<Button>(R.id.editRedo).isEnabled = false


                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            findViewById<Button>(R.id.editReplaceAll).setOnClickListener{ view ->
                try {


                    val searchPattern = findViewById<EditText>(R.id.editSearchPattern)?.text?.toString()!!
                    val replaceText = findViewById<EditText>(R.id.editReplaceText)?.text?.toString()!!

                    if (searchPattern.isEmpty()) {
                        Toast.makeText(applicationContext, getString(R.string.empty_search_pattern), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    var searchRegex: Regex? = null
                    try {
                        searchRegex = Regex(searchPattern)
                    } catch (exception: Exception) {
                        Toast.makeText(applicationContext, getString(R.string.wrong_search_pattern) + exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }

                    var text = editText?.text?.toString()!!

                    if (searchRegex?.find(text) == null) {
                        Toast.makeText(applicationContext, getString(R.string.search_result_zero), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    text = text.replace(Regex(searchPattern), replaceText)

                    while (historyIndex!! < historyList?.size!! - 1) {
                        historyList?.removeLast()
                    }

                    var cursorIndex = editText?.selectionEnd ?: 0
                    if (text?.length!! < cursorIndex) {
                        cursorIndex = text?.length!!
                    }
                    historyList?.add(History(text,  cursorIndex))
                    historyIndex = historyList?.size!! - 1

                    editText?.setText(text)
                    editText?.setSelection(cursorIndex)

                    fileNameTextView?.text = editingFileName

                    findViewById<Button>(R.id.editUndo).isEnabled = true
                    findViewById<Button>(R.id.editRedo).isEnabled = false

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            editText?.addTextChangedListener(this)


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

            editText = null


            fileName = null
            editingFileName = null


            historyList?.clear()
            historyList = null

            historyIndex = null


            saveActivityResultLauncher?.unregister()
            saveActivityResultLauncher = null


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {

            super.onDestroy()
        }
    }


    //EditTextのtextが変更されている時に1度だけ、変更前のtextなどを渡されて起動されます。
    override fun beforeTextChanged(text: CharSequence?, index: Int, changedSize: Int, addedSize: Int) {
    }


    //EditTextのtextが変更されている時に変更のたびに、変更中のtextなどを渡されて起動されます。
    override fun onTextChanged(text: CharSequence?, index: Int, deletedSize: Int, addedSize: Int) {
    }


    //EditTextのtextが変更された時に、変更後のtextを渡されて起動されます。
    override fun afterTextChanged(text: Editable?) {
        try {

            if (historyIndex!! <= -1) {
                return
            }

            val lastText = historyList?.get(historyIndex!!)?.text!!

            if (lastText == text?.toString()) {
                return
            }

            while (historyIndex!! < historyList?.size!! - 1) {
                historyList?.removeLast()
            }

            historyList?.add(History(text?.toString() ?: "", editText?.selectionEnd ?: text?.toString()?.length ?: 0))
            historyIndex = historyList?.size!! - 1

            fileNameTextView?.text = editingFileName

            findViewById<Button>(R.id.editUndo).isEnabled = true
            findViewById<Button>(R.id.editRedo).isEnabled = false

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }
}
