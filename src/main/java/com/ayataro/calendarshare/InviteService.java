package com.ayataro.calendarshare;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InviteService {

    private final InviteRepository repo;
    private final UserRepository userRepo;

    public InviteService(InviteRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public void invite(User sender, String email) {

        User receiver = userRepo.findByEmail(email).orElseThrow();

        Invite i = new Invite();
        i.setSender(sender);
        i.setReceiver(receiver);
        i.setStatus(Invite.Status.PENDING);
        repo.save(i);
    }

    public void accept(User me, Long id) {
        Invite i = repo.findById(id).orElseThrow();
        if (!i.getReceiver().equals(me)) return;
        i.setStatus(Invite.Status.ACCEPTED);
        repo.save(i);
    }

    public List<InviteDTO> listInvitesForDashboard(User me) {
        return repo.findBySenderOrReceiver(me, me).stream()
                .map(i -> {
                    boolean isReceiver = i.getReceiver().equals(me);
                    String partnerEmail = isReceiver
                            ? i.getSender().getEmail()
                            : i.getReceiver().getEmail();

                    return new InviteDTO(
                            i.getId(),
                            partnerEmail,
                            i.getStatus().name(),
                            isReceiver && i.getStatus() == Invite.Status.PENDING
                    );
                }).toList();
    }

    public List<User> listAcceptedPartners(User me) {
        return repo.findBySenderOrReceiver(me, me).stream()
                .filter(i -> i.getStatus() == Invite.Status.ACCEPTED)
                .map(i -> i.getSender().equals(me) ? i.getReceiver() : i.getSender())
                .toList();
    }
    private Invite requirePendingInviteForReceiver(Long id, User me) {
        Invite i = repo.findById(id).orElseThrow();

        if (i.getReceiver() == null || i.getReceiver().getId() == null || !i.getReceiver().getId().equals(me.getId())) {
            throw new IllegalStateException("Not allowed");
        }
        if (i.getStatus() != Invite.Status.PENDING) {
            throw new IllegalStateException("Invite is not pending");
        }
        return i;
    }

    public void accept(Long id, User me) {
        Invite i = repo.findById(id).orElseThrow();
        if (!i.getReceiver().equals(me)) {
            throw new IllegalStateException("Not allowed");
        }
        i.setStatus(Invite.Status.ACCEPTED);
        repo.save(i);
    }

    public void reject(Long id, User me) {
        Invite i = repo.findById(id).orElseThrow();
        if (!i.getReceiver().equals(me)) {
            throw new IllegalStateException("Not allowed");
        }
        i.setStatus(Invite.Status.REJECTED);
        repo.save(i);
    }
}
