package boids

import cs214.*

class BoidTest extends munit.FunSuite:

  test("world with single boid, no forces"):
    runTestCase("00_singleBoidNoForces")

  test("world with three boids, no forces"):
    runTestCase("01_threeBoidsNoForces")

  test("avoidance doesn't affect lone boid"):
    runTestCase("10_singleBoidAvoidance")

  test("avoidance between two boids face-to-face"):
    runTestCase("11_twoBoidsAvoidanceX")

  test("same as above, with orthogonal velocity component"):
    runTestCase("12_twoBoidsAvoidanceXY")

  test("no avoidance between far boids"):
    runTestCase("13_twoBoidsAvoidanceFar")

  test("avoidance among mixed boids"):
    runTestCase("14_mixedAvoidance")

  test("avoidance among boids at the same position"):
    runTestCase("15_avoidanceSamePosition")

  test("only cohesion between two boids"):
    runTestCase("20_twoBoidsRestCohesion")

  test("cohesion makes two boids dance"):
    runTestCase("21_twoBoidsCohesionDance")

  test("no cohesion between two far boids"):
    runTestCase("22_twoBoidsCohesionFar")

  test("cohesion can be chaotic"):
    runTestCase("23_chaoticCohesion")

  test("cohesion outside avoidance range"):
    runTestCase("30_avoidanceCohesion")

  test("cohesion within avoidance range"):
    runTestCase("31_avoidanceCohesionLonger")

  test("the three-body problem"):
    runTestCase("32_threeBodyProblem")

  test("only alignment"):
    runTestCase("40_onlyAlignment")

  test("no alignment between far boids"):
    runTestCase("41_alignmentFar")

  test("containment above top"):
    runTestCase("50_containmentTop")

  test("containment below bottom"):
    runTestCase("51_containmentBottom")

  test("containment beyond left"):
    runTestCase("52_containmentLeft")

  test("containment beyond right"):
    runTestCase("53_containmentRight")

  test("cumulative containment"):
    runTestCase("54_containmentCumulative")

  test("all forces together on many boids"):
    runTestCase("60_allTogether")
