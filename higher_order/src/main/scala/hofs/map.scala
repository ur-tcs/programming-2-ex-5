package hofs

/* The following functions relate to the "map" category */

// This function creates a new list whose values correspond to the ones in the original list incremented by one:
def increment(l: IntList): IntList ={
  if l.isEmpty then IntNil()
  else IntCons(l.head + 1, increment(l.tail))
}

// This function creates a copy of a list with all numbers greater than 0 replaced with zeroes:
def capAtZero(l: IntList): IntList ={
  if l.isEmpty then IntNil()
  else IntCons(if l.head > 0 then 0 else l.head, capAtZero(l.tail))
}


