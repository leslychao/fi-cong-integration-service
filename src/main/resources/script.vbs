Dim args, objExcel
Set args = WScript.Arguments
Set objExcel = CreateObject("Excel.Application")
objExcel.DisplayAlerts=false
objExcel.Workbooks.Open args(0), Password="89099890911"
objExcel.Application.Visible=false
objExcel.WorkSheets(1).Activate
objExcel.Activeworkbook.SaveAs args(0)
objExcel.Activeworkbook.close(0)
objExcel.Application.Visible=true
objExcel.DisplayAlerts=true
objExcel.Application.Quit
WScript.Echo "Finished."
WScript.Quit
