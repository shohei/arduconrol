char recv;
char buf[128];

void setup()
{
  pinMode(13,OUTPUT);
  Serial.begin(9600);
}

void loop()
{
  while(Serial.available() > 0){
    recv = Serial.read();
    Serial.print(recv);
    if(recv=='o'){
      digitalWrite(13,HIGH);
    } else if(recv=='x'){
      digitalWrite(13,LOW);
    }
  }
}
