Dim args, objExcel
Set args = WScript.Arguments
Set objExcel = CreateObject("Excel.Application")
sExcelPath = args(0)
objExcel.Workbooks.Open sExcelPath, Password="89099890911", ReadOnly=False
objExcel.Application.Visible = False
objExcel.WorkSheets(1).Activate
objExcel.Activeworkbook.Save
objExcel.Activeworkbook.close(0)
objExcel.Application.Quit
WScript.Echo "Finished."
WScript.Quit
