package iuh.fit.se.repository;

import iuh.fit.se.entity.Notification;
import iuh.fit.se.entity.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Tìm kiếm thông báo của một người dùng và phân trang kết quả.
     * Sắp xếp theo thời gian tạo mới nhất lên đầu.
     * @param userId ID của người dùng
     * @param pageable Thông tin phân trang (trang số mấy, bao nhiêu mục mỗi trang)
     * @return Một trang (Page) chứa danh sách các thông báo
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Tìm tất cả các thông báo của một người dùng với một trạng thái cụ thể (ví dụ: UNREAD).
     * Sắp xếp theo thời gian tạo mới nhất lên đầu.
     * @param userId ID của người dùng
     * @param status Trạng thái cần tìm (READ/UNREAD)
     * @return Danh sách các thông báo thỏa mãn điều kiện
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationStatus status);

    /**
     * Đếm số lượng thông báo của một người dùng với một trạng thái cụ thể.
     * Rất hữu ích để hiển thị số lượng thông báo chưa đọc trên giao diện.
     * @param userId ID của người dùng
     * @param status Trạng thái cần đếm
     * @return Số lượng thông báo
     */
    long countByUserIdAndStatus(String userId, NotificationStatus status);
}