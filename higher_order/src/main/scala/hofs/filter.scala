package hofs

/* The following functions relate to the "filter" category */

// This function collects all even values from an IntList:
def collectEven(l: IntList): IntList ={
  if l.isEmpty then IntNil()
  else if l.head % 2 == 0 then
  IntCons(l.head, collectEven(l.tail))
  else collectEven(l.tail)
}

// This function creates a copy of a list with all zeroes removed:
def removeZeroes(l: IntList): IntList ={
  if l.isEmpty then IntNil()
  else 
      if l.head == 0 then removeZeroes(l.tail)
      else IntCons(l.head, removeZeroes(l.tail))
}


