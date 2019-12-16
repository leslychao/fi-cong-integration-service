Dim args, objExcel
Set args = WScript.Arguments
Set objExcel = CreateObject("Excel.Application")
objExcel.DisplayAlerts=false
objExcel.Workbooks.Open args(0),false,false,,"89099890911","89099890911",,,,true
objExcel.Application.Visible=false
objExcel.WorkSheets(1).Activate
objExcel.Activeworkbook.SaveAs args(0),51, , , , , ,xlLocalSessionChanges
objExcel.Activeworkbook.close(true)
objExcel.DisplayAlerts=true
objExcel.Application.Quit
WScript.Echo "Finished."
WScript.Quit
