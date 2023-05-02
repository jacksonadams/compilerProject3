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
.globl  fact
fact:
fact_bb2:
	pushq	%R15
	movl	%EDI, %R15D
fact_bb3:
	movl	$1, %EAX
	cmpl	%EAX, %R15D
	jle	fact_bb5
fact_bb4:
	movl	$1, %ESI
	movl	%R15D, %EDI
	subl	%ESI, %EDI
	call	fact
	movl	%EAX, %EDI
	movl	%R15D, %EAX
	imull	%EDI, %EAX
fact_bb1:
	popq	%R15
	ret
fact_bb5:
	movl	$0, %EAX
	cmpl	%EAX, %R15D
	jge	fact_bb8
	jmp	fact_bb1
fact_bb8:
	movl	$1, %EAX
	jmp	fact_bb1
	jmp	fact_bb1
.globl  main
main:
main_bb2:
main_bb3:
	movl	$5, %EDI
	call	fact
	movl	%EAX, %EDI
	call	printInt
	movl	$10, %EAX
	movl	%EAX, %EDI
	call	putchar
	movl	$0, %EAX
main_bb1:
	ret
