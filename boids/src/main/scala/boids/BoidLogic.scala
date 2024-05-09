package boids
import p2.{Vector2, BoidSequence}

def boidsWithinRadius(thisBoid: Boid, boids: BoidSequence, radius: Float): BoidSequence =
  boids

def avoidanceForce(thisBoid: Boid, boidsWithinAvoidanceRadius: BoidSequence): p2.Vector2 =
  p2.Vector2.Zero

def cohesionForce(thisBoid: Boid, boidsWithinPerceptionRadius: BoidSequence): p2.Vector2 =
  p2.Vector2.Zero

def alignmentForce(thisBoid: Boid, boidsWithinPerceptionRadius: BoidSequence): p2.Vector2 =
  p2.Vector2.Zero

def containmentForce(thisBoid: Boid, allBoids: BoidSequence, width: Int, height: Int): p2.Vector2 =
  p2.Vector2.Zero

def totalForce(thisBoid: Boid, allBoids: BoidSequence, physics: Physics): Vector2 =
  val withinPerceptionRadius = boidsWithinRadius(thisBoid, allBoids, physics.perceptionRadius)
  val cohere = cohesionForce(thisBoid, withinPerceptionRadius)
  val align = alignmentForce(thisBoid, withinPerceptionRadius)
  val withinAvoidanceRadius = boidsWithinRadius(thisBoid, withinPerceptionRadius, physics.avoidanceRadius)
  val avoid = avoidanceForce(thisBoid, withinAvoidanceRadius)
  val contain = containmentForce(thisBoid, allBoids, physics.WIDTH, physics.HEIGHT)
  val total =
    avoid * physics.avoidanceWeight +
      cohere * physics.cohesionWeight +
      align * physics.alignmentWeight +
      contain * physics.containmentWeight
  total


def tickBoid(thisBoid: Boid, allBoids: BoidSequence, physics: Physics): Boid =
  val acceleration = totalForce(thisBoid, allBoids, physics)
  Boid(thisBoid.position, thisBoid.velocity)

def tickWorld(allBoids: BoidSequence, physics: Physics): BoidSequence =
  allBoids
