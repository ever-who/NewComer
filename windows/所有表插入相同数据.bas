Sub m()
k = 0
For Each sh In Sheets
    sh.Cells(1, 1) = "App Name"
    sh.Cells(1, 2) = "String Name"
    sh.Cells(1, 4) = "English(Default)"
    sh.Cells(1, 6) = sh.Name
    If (k > 0) Then
        Sheet1.Range("A2:D18").Copy sh.Range("A2:D18")
    End If
    k = k + 1
    Next
End Sub
