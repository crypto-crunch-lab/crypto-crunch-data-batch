package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.common.feign.client.CoinDixApiClient;
import com.crypto.crunch.batch.common.feign.client.CoreApiClient;
import com.crypto.crunch.batch.defi.conf.ImageConf;
import com.crypto.crunch.batch.defi.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefiSyncServiceImpl implements DefiSyncService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;
    private final CoreApiClient coreApiClient;

    private static final String YMDT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DEFI_INDEX = "defi";

    public DefiSyncServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper, CoreApiClient coreApiClient) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
        this.coreApiClient = coreApiClient;
    }

    @Override
    public void sync() throws IOException {
        int pageNo = 0;
        List<Defi> defiList = new ArrayList<>();
        while (true) {
            CoinDixApiResponse response = coinDixApiClient.getList(pageNo);
            List<Defi> currList = response.getData().stream().map(this::mapToDefi).collect(Collectors.toList());
            defiList.addAll(currList);
            pageNo++;

            if (!response.getHasNextPage()) {
                break;
            }
        }
        log.info("total count: {}", defiList.size());

        BulkRequest bulkRequest = new BulkRequest();
        for (Defi defi : defiList) {

            GetRequest getRequest = new GetRequest(DEFI_INDEX, defi.getId());
            GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

            if (getResponse.isExists()) {
                defi.setUpdateYmdt(DateTime.now().toString(YMDT_TIME_FORMAT));

                UpdateRequest updateRequest = new UpdateRequest(DEFI_INDEX, defi.getId());
                updateRequest.doc(objectMapper.writeValueAsString(defi), XContentType.JSON);
                bulkRequest.add(updateRequest);
            } else {
                defi.setSyncYmdt(DateTime.now().toString(YMDT_TIME_FORMAT));
                defi.setUpdateYmdt(DateTime.now().toString(YMDT_TIME_FORMAT));

                IndexRequest indexRequest = new IndexRequest(DEFI_INDEX).id(defi.getId());
                indexRequest.source(objectMapper.writeValueAsString(defi), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info(bulkResponse.toString());
    }

    private Defi mapToDefi(CoinDixDefi coinDixDefi) {
        return Defi.builder()
                .id(coinDixDefi.getId())
                .name(coinDixDefi.getName())
                .platform(coinDixDefi.getProtocol())
                .network(coinDixDefi.getChain())
                .base(coinDixDefi.getBase())
                .reward(coinDixDefi.getReward())
                .apy(coinDixDefi.getApy())
                .tvl(coinDixDefi.getTvl())
                .risk(coinDixDefi.getRisk())
                .defiIconUrl(coinDixDefi.getIcon())
                .detailUrl(coinDixDefi.getLink())
                .build();
    }

    private String getDefiIconUrl(String id, String url) {
        try {
            BufferedImage image = ImageIO.read(new URL("https://crypto-crunch-static.s3.ap-northeast-2.amazonaws.com/image/icon/defi/" + id + ".png"));
            if (!ObjectUtils.isEmpty(image)) {
                return "https://crypto-crunch-static.s3.ap-northeast-2.amazonaws.com/image/icon/defi/" + id + ".png";
            }
        } catch (IOException e) {
            log.info("image file not exist");
        }

        ImageUploadRequest request = new ImageUploadRequest();
        request.setUrl(url);
        request.setUploadType(ImageConf.ImageUploadType.URL);
        request.setDirName("image/icon/defi");
        request.setFileName(id + ".png");

        return coreApiClient.uploadImage(request).getBody();
    }
}
