package testSupport.preConditions

import testSupport.ITCoreTestData.{SDIL_REF, UTR}

trait PreconditionHelpers {
  implicit val builder: PreconditionBuilder

  // valid user preconditions
  def commonPrecondition = {
    builder
      .user.isAuthorisedAndEnrolled
      .sdilBackend.retrieveSubscription("utr", UTR)
  }

  def commonPreconditionBoth = {
    builder
      .user.isAuthorisedAndEnrolledBoth
      .sdilBackend.retrieveSubscription("utr", UTR)
      .sdilBackend.retrieveSubscription("sdil", SDIL_REF)
  }
  def commonPreconditionSdilRef = {
    builder
      .user.isAuthorisedAndEnrolledSDILRef
      .sdilBackend.retrieveSubscription("sdil", SDIL_REF)
  }

  def authorisedWithSdilSubscriptionIncDeRegDatePrecondition = {
    builder
      .user.isAuthorisedAndEnrolled
      .sdilBackend.retrieveSubscriptionWithDeRegDate("utr", UTR)
  }


  def authorisedWithNoSubscriptionPrecondition = {
    builder
      .user.isAuthorisedAndEnrolled
      .sdilBackend.retrieveSubscriptionNone("utr", UTR)
  }

  def authorisedButNoEnrolmentsPrecondition = {
    builder
      .user.isAuthorisedButNotEnrolled()
  }

  //invalid user preconditions


  def authorisedWithSdilSubscriptionNoDeRegDatePrecondition = {
    builder
      .user.isAuthorisedAndEnrolled
      .sdilBackend.retrieveSubscription("utr", UTR)
  }

  def authorisedWithInvalidRolePrecondition  = {
    builder
      .user.isAuthorisedWithInvalidRole
  }

  def authorisedWithInvalidAffinityPrecondition = {
    builder
      .user.isAuthorisedButInvalidAffinity
  }

  def unauthorisedPrecondition = {
    builder
      .user.isNotAuthorised()
  }

  def authorisedButInternalIdPrecondition = {
    builder
      .user.isAuthorisedWithMissingInternalId
  }
}
