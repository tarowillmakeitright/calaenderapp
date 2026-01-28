package com.ayataro.calendarshare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    List<Invite> findBySenderOrReceiver(User s, User r);
    List<Invite> findByReceiverAndStatus(User r, Invite.Status status);
    List<Invite> findBySenderAndStatus(User s, Invite.Status status);
}