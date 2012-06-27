@echo off

if "%1"=="" (
  call clean
  lein2 with-profile dev,1.4 test
  call clean
  goto eof
)

call clean
lein2 with-profile dev,%1 test
call clean

:eof

