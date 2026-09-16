package com.example

import com.example.data.entity.Resident
import com.example.data.entity.UserRole
import com.example.data.entity.isMember
import com.example.data.entity.withProfileEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a member may change about themselves.
 *
 * The rule the committee asked for is narrow and worth pinning down: a member
 * fills in what the society never recorded, and touches nothing else. Everyone
 * else editing their own profile is unaffected.
 */
class MemberProfileEditTest {

    @Test
    fun `a member cannot overwrite a detail the society already holds`() {
        val onFile = member(ownerName = "Sunil Patil", contactNumber = "9876543210")

        val edited = onFile.withProfileEdits(
            ownerName = "Somebody Else",
            coApplicantName = "",
            contactNumber = "9000000000",
            email = "",
            photoBase64 = "",
            addOnly = true
        )

        assertEquals("Sunil Patil", edited.ownerName)
        assertEquals("9876543210", edited.contactNumber)
    }

    @Test
    fun `a member can fill in what is missing`() {
        val onFile = member(ownerName = "Sunil Patil", email = "")

        val edited = onFile.withProfileEdits(
            ownerName = "Sunil Patil",
            coApplicantName = "Asha Patil",
            contactNumber = "",
            email = "sunil@example.com",
            photoBase64 = "",
            addOnly = true
        )

        assertEquals("sunil@example.com", edited.email)
        assertEquals("Asha Patil", edited.coApplicantName)
    }

    @Test
    fun `a co-applicant already on record cannot be renamed by the member`() {
        val onFile = member(coApplicantName = "Asha Patil")

        val edited = onFile.withProfileEdits(
            ownerName = "",
            coApplicantName = "Someone Else",
            contactNumber = "",
            email = "",
            photoBase64 = "",
            addOnly = true
        )

        assertEquals("Asha Patil", edited.coApplicantName)
    }

    @Test
    fun `a photo already uploaded is not replaced, and a missing one can be added`() {
        val withPhoto = member(photoBase64 = "existing-photo")
        val withoutPhoto = member(photoBase64 = "")

        assertEquals(
            "existing-photo",
            withPhoto.withProfileEdits("", "", "", "", "new-photo", addOnly = true).photoBase64
        )
        assertEquals(
            "new-photo",
            withoutPhoto.withProfileEdits("", "", "", "", "new-photo", addOnly = true).photoBase64
        )
    }

    @Test
    fun `a member clearing a field they filled in cannot blank the register`() {
        val onFile = member(email = "sunil@example.com")

        val edited = onFile.withProfileEdits("", "", "", "", "", addOnly = true)

        assertEquals("sunil@example.com", edited.email)
    }

    @Test
    fun `the name is never blanked, even for somebody editing freely`() {
        val onFile = member(ownerName = "Sunil Patil")

        val edited = onFile.withProfileEdits("   ", "", "9876543210", "", "", addOnly = false)

        assertEquals("Sunil Patil", edited.ownerName)
    }

    @Test
    fun `the committee and the staff still edit their own details freely`() {
        val chairman = member(
            role = UserRole.CHAIRMAN,
            ownerName = "Sunil Patil",
            contactNumber = "9876543210"
        )

        val edited = chairman.withProfileEdits(
            ownerName = "Sunil M Patil",
            coApplicantName = "",
            contactNumber = "9000000000",
            email = "chair@example.com",
            photoBase64 = "",
            addOnly = chairman.role.isMember
        )

        assertEquals("Sunil M Patil", edited.ownerName)
        assertEquals("9000000000", edited.contactNumber)
        assertEquals("chair@example.com", edited.email)
    }

    @Test
    fun `whitespace around a newly added detail is trimmed off`() {
        val onFile = member(email = "")

        val edited = onFile.withProfileEdits("", "", "", "  sunil@example.com  ", "", addOnly = true)

        assertEquals("sunil@example.com", edited.email)
    }

    @Test
    fun `a member whose record is complete changes nothing at all`() {
        val onFile = member(
            ownerName = "Sunil Patil",
            coApplicantName = "Asha Patil",
            contactNumber = "9876543210",
            email = "sunil@example.com",
            photoBase64 = "photo"
        )

        val edited = onFile.withProfileEdits("A", "B", "C", "D", "E", addOnly = true)

        assertEquals(onFile, edited)
    }

    @Test
    fun `residents and tenants are members, the committee and staff are not`() {
        assertTrue(UserRole.RESIDENT.isMember)
        assertTrue(UserRole.TENANT.isMember)
        assertFalse(UserRole.CHAIRMAN.isMember)
        assertFalse(UserRole.TREASURER.isMember)
        assertFalse(UserRole.GUARD.isMember)
    }

    private fun member(
        role: UserRole = UserRole.RESIDENT,
        ownerName: String = "Sunil Patil",
        coApplicantName: String = "",
        contactNumber: String = "",
        email: String = "",
        photoBase64: String = ""
    ) = Resident(
        docId = "A-202",
        flatNumber = "A-202",
        wing = "A",
        ownerName = ownerName,
        coApplicantName = coApplicantName,
        contactNumber = contactNumber,
        email = email,
        photoBase64 = photoBase64,
        role = role
    )
}
