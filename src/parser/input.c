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

void checkGuess(int guess){
	int response;
	putchar (103);
	putchar (117);
	putchar (101);
	putchar (115);
	putchar (115);
	putchar (61);
	printInt(guess);
	putchar (10);
	response = getchar();
	getchar();
	if (response == 76) {
		return 0-1;
	}
	else if (response == 72) {
		return 1;
	}
	else {
		return 0;
	}
}

void binomial(void){
	int low;
	int high;	
    int guess;
	int result;
	low = 0;
	high = 100;
	
	result = 1;
	while (result != 0){
		guess = (low+high) / 2;
		result = checkGuess(guess);
		if (result > 0){
			high = guess;
		}
		else if (result < 0){
			low = guess;
		}
	}
}


int main( void )
{ 
  binomial();
  return 0;
}