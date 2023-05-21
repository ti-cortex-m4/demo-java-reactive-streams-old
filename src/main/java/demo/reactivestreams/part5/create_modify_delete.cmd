cd %HOMEPATH%

timeout /t 10
type nul > example.txt

timeout /t 10
echo text >> example.txt

timeout /t 10
del example.txt
