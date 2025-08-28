Basic Read Holding Registers
modpoll -m tcp -a 1 -r 1 -c 5 -p 50200 127.0.0.1
Read Coils
modpoll -m tcp -a 1 -r 1 -c 5 -t 0 -p 50200 127.0.0.1
Write a Single Holding Register
writes value 123 into holding register 1
modpoll -m tcp -a 1 -r 1 -t 4 -p 50200 127.0.0.1
Write a Single Coil
writes ON (1) into coil 1
modpoll -m tcp -a 1 -r 1 -t 0 -p 50200 127.0.0.1