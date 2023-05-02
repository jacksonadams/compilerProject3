void putDigit(int s) {
   putchar(48+s);
}

void printInt(int x) {
	int y;
	int z;
	if (x < 0){
		putchar(45);
		x = 0 - x;
	}
	z = x;
	x = x / 10;
	y = z - x * 10;
	if (x > 0){
		printInt(x);
	}
	
    putDigit(y);
}

int fact(int x){ 
  if (x > 1) {
	return x * fact(x-1);
  }
  else if (x < 0){
	putchar (66);
	putchar (65);
	putchar (68);
	return 0-1;
  }
  else {  
	return 1;
  }
}

int main( void )
{ int x;
  x = 5;
  printInt(fact(x));
  putchar(10);
  return 0;
}