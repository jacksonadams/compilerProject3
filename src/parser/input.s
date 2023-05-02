.text
	.align 4
.globl  putDigit
putDigit:
putDigit_bb2:
putDigit_bb3:
	movl	$48, %EAX
	addl	%EDI, %EAX
	movl	%EAX, %EDI
	call	putchar
putDigit_bb1:
	ret
.globl  printInt
printInt:
printInt_bb2:
	pushq	%R14
	pushq	%R15
	movl	%EDI, %R14D
printInt_bb3:
	movl	$0, %EAX
	cmpl	%EAX, %R14D
	jge	printInt_bb5
printInt_bb4:
	movl	$45, %EAX
	movl	%EAX, %EDI
	call	putchar
	movl	$0, %EAX
	subl	%R14D, %EAX
	movl	%EAX, %R14D
printInt_bb5:
	movl	%R14D, %ESI
	movl	$10, %EDI
	movl	$0, %EDX
	movl	%R14D, %EAX
	idivl	%EDI, %EAX
	movl	%EAX, %R14D
	movl	$10, %EDI
	movl	%R14D, %EAX
	imull	%EDI, %EAX
	movl	%EAX, %EDI
	movl	%ESI, %EAX
	subl	%EDI, %EAX
	movl	%EAX, %R15D
	movl	$0, %EAX
	cmpl	%EAX, %R14D
	jle	printInt_bb7
printInt_bb6:
	movl	%R14D, %EDI
	call	printInt
printInt_bb7:
	movl	%R15D, %EDI
	call	putDigit
printInt_bb1:
	popq	%R15
	popq	%R14
	ret
.globl  checkGuess
checkGuess:
checkGuess_bb2:
	pushq	%R15
	movl	%EDI, %R15D
checkGuess_bb3:
	movl	$103, %EDI
	call	putchar
	movl	$117, %EDI
	call	putchar
	movl	$101, %EDI
	call	putchar
	movl	$115, %EDI
	call	putchar
	movl	$115, %EDI
	call	putchar
	movl	$61, %EDI
	call	putchar
	movl	%R15D, %EDI
	call	printInt
	movl	$10, %EDI
	call	putchar
	call	getchar
	movl	%EAX, %R15D
	call	getchar
	movl	$76, %EAX
	cmpl	%EAX, %R15D
	jne	checkGuess_bb5
checkGuess_bb4:
	movl	$0, %EAX
	movl	$1, %EDI
	subl	%EDI, %EAX
checkGuess_bb1:
	popq	%R15
	ret
checkGuess_bb8:
	movl	$0, %EAX
	jmp	checkGuess_bb1
	jmp	checkGuess_bb1
checkGuess_bb5:
	movl	$72, %EAX
	cmpl	%EAX, %R15D
	jne	checkGuess_bb8
checkGuess_bb7:
	movl	$1, %EAX
	jmp	checkGuess_bb1
.globl  binomial
binomial:
binomial_bb2:
	pushq	%R12
	pushq	%R13
	pushq	%R14
	pushq	%R15
binomial_bb3:
	movl	$0, %EAX
	movl	%EAX, %R13D
	movl	$100, %EAX
	movl	%EAX, %R14D
	movl	$1, %EAX
binomial_bb4:
	cmpl	$0, %R12D
	je	binomial_bb1
binomial_bb6:
	movl	%R13D, %EAX
	addl	%R14D, %EAX
	movl	$2, %EDI
	movl	$0, %EDX
	idivl	%EDI, %EAX
	movl	%EAX, %EDI
	movl	%EDI, %R15D
	movl	%R15D, %EDI
	call	checkGuess
	movl	%EAX, %EDI
	movl	$0, %EAX
	cmpl	%EAX, %EDI
	jle	binomial_bb8
	jmp	binomial_bb4
binomial_bb1:
	popq	%R15
	popq	%R14
	popq	%R13
	popq	%R12
	ret
binomial_bb8:
	movl	$0, %EAX
	cmpl	%EAX, %EDI
	jge	binomial_bb1
binomial_bb10:
binomial_bb11:
	jmp	binomial_bb1
.globl  main
main:
main_bb2:
main_bb3:
	call	binomial
	movl	$0, %EAX
main_bb1:
	ret
