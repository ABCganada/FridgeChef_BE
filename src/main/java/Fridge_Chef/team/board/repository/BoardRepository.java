package Fridge_Chef.team.board.repository;

import Fridge_Chef.team.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
}