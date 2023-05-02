(FUNCTION  addThem  [(int a) (int b)]
  (BB 2
    (OPER 3 Func_Entry []  [])
    (OPER 4 GT [(r 5)]  [(r 3)(r 4)])
    (OPER 5 Mov [(r 1)]  [(r 5)])
    (OPER 6 Return [(r 0)]  [(r 1)])
    (OPER 7 Jmp [(bb 1)]  [(bb 2)])
  )
)
