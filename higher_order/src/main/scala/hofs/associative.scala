package hofs

/* The following functions relate to the "associative" category */

// This function computes the sum of all elements in an IntList:
def sum(l: IntList): Int = {
  if l.isEmpty then 0
  else l.head + sum(l.tail)
}

// This function computes the product of all elements in an IntList:
def product(l: IntList): Int ={
  if l.isEmpty then 1
  else l.head * product(l.tail)
}

