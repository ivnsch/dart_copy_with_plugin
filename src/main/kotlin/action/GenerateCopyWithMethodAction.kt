package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager

class GenerateCopyWithMethodAction : AnAction("Generate copyWith") {

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isVisible =
            event.project != null // should not be null if there's a file extension, but let's be sure
            && editor?.let { FileDocumentManager.getInstance().getFile(it.document)?.extension } == "dart"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val toInsert = CopyWithMethodGenerator().generate(editor.document.charsSequence) ?: return
        WriteCommandAction.runWriteCommandAction(event.project) {
            editor.document.insertString(editor.caretModel.currentCaret.offset, toInsert)
        }
    }
}
