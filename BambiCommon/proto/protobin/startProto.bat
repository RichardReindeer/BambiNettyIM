cd ./
:: -I后接proto文件所在目录，需要跟最后的参数 (携带proto文件名字的目录保持一直)
protoc.exe -I=../protoConfig/ --java_out=../../src/main/java ../protoConfig/Message.proto
pause