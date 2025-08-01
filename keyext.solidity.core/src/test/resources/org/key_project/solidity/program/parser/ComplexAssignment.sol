contract ComplexAssignment {
   function func(uint256 u, uint256 v, uint256 w) public pure  {
       v += w = u -= 1;
   }
}
