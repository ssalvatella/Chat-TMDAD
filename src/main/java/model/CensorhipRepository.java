package model;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CensorhipRepository extends MongoRepository<Censorship, String> {

    Censorship findByWord(String word);
}
