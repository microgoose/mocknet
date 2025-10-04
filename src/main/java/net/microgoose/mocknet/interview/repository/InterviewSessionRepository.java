package net.microgoose.mocknet.interview.repository;

import net.microgoose.mocknet.interview.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {
    Optional<InterviewSession> findBySlotBookingId(UUID slotBookingId);
}
