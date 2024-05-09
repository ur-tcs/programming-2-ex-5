package hofs

/* The following functions relate to the "foldRight" category */

// This function counts the number of even values in an IntList:
def countEven(l: IntList): Int ={
  if l.isEmpty then 0
  else (if l.head % 2 == 0 then 1 else 0) + countEven(l.tail)
}

// This function computes the product of all odd numbers in an IntList:
def multiplyOdd(l: IntList): Int ={
  if l.isEmpty then 1
  else
    val m = if l.head % 2 != 0
    then l.head else 1
    m * multiplyOdd(l.tail)
}